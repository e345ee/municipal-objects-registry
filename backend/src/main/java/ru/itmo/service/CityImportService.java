package ru.itmo.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.postgresql.util.PSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.dto.CityDto;
import ru.itmo.dto.ImportResultDto;
import ru.itmo.exception.ImportValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class CityImportService {

    private final CityService cityService;
    private final Validator validator;
    private final ImportOperationService importOpService;
    private final PlatformTransactionManager txManager;

    public CityImportService(CityService cityService,
                             Validator validator,
                             ImportOperationService importOpService,
                             PlatformTransactionManager txManager) {
        this.cityService = cityService;
        this.validator = validator;
        this.importOpService = importOpService;
        this.txManager = txManager;
    }

    public ImportResultDto importCities(List<CityDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new IllegalArgumentException("Файл не содержит записей для импорта.");
        }

        var op = importOpService.start();

        List<ImportValidationException.ItemError> errors = validateAll(dtos);
        if (!errors.isEmpty()) {
            importOpService.markFailed(op.getId(), "Import validation failed");
            throw new ImportValidationException(errors);
        }

        int maxAttempts = 3;
        long backoffMs = 30;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ImportResultDto res = runSerializable(() -> doImportTransactional(dtos, op.getId()));
                return res;
            } catch (RuntimeException ex) {
                boolean serFail = isSerializationFailure(ex);

                if (serFail && attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    backoffMs *= 2;
                    continue;
                }

                importOpService.markFailed(op.getId(), safeMsg(ex));
                throw ex;
            }
        }

        throw new IllegalStateException("Unreachable code");
    }

    private ImportResultDto doImportTransactional(List<CityDto> dtos, Long opId) {
        List<Long> createdIds = new ArrayList<>(dtos.size());

        for (CityDto dto : dtos) {
            CityDto created = cityService.create(dto);
            createdIds.add(created.getId());
        }

        importOpService.markSuccess(opId, createdIds.size());

        return new ImportResultDto(createdIds.size(), createdIds);
    }

    private List<ImportValidationException.ItemError> validateAll(List<CityDto> dtos) {
        List<ImportValidationException.ItemError> errors = new ArrayList<>();

        for (int i = 0; i < dtos.size(); i++) {
            CityDto dto = dtos.get(i);

            if (dto == null) {
                errors.add(new ImportValidationException.ItemError(i, "$", "Запись равна null"));
                continue;
            }

            addViolations(errors, i, validator.validate(dto));

            if (dto.getCoordinates() != null) {
                addViolations(errors, i, validator.validate(dto.getCoordinates()));
            }
            if (dto.getGovernor() != null) {
                addViolations(errors, i, validator.validate(dto.getGovernor()));
            }

            if ((dto.getCoordinatesId() == null) == (dto.getCoordinates() == null)) {
                errors.add(new ImportValidationException.ItemError(
                        i, "coordinates",
                        "Укажи либо coordinatesId, либо coordinates (ровно одно)."
                ));
            }
            if (dto.getGovernorId() != null && dto.getGovernor() != null) {
                errors.add(new ImportValidationException.ItemError(
                        i, "governor",
                        "Укажи либо governorId, либо governor (не оба)."
                ));
            }
        }

        return errors;
    }

    private void addViolations(List<ImportValidationException.ItemError> errors,
                               int index,
                               Set<? extends ConstraintViolation<?>> violations) {
        for (ConstraintViolation<?> v : violations) {
            String field = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "$";
            errors.add(new ImportValidationException.ItemError(index, field, v.getMessage()));
        }
    }

    private String safeMsg(RuntimeException ex) {
        String m = ex.getMessage();
        if (m == null || m.isBlank()) return ex.getClass().getSimpleName();
        return m.length() > 1000 ? m.substring(0, 1000) : m;
    }

    private <T> T runSerializable(Supplier<T> action) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        return tt.execute(status -> action.get());
    }

    private boolean isSerializationFailure(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        if (t instanceof PSQLException psql) {
            return "40001".equals(psql.getSQLState());
        }
        return false;
    }
}
