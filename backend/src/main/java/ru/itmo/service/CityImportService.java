package ru.itmo.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.dto.CityDto;
import ru.itmo.dto.ImportResultDto;
import ru.itmo.exception.ImportValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CityImportService {

    private final CityService cityService;
    private final Validator validator;
    private final ImportOperationService importOpService;

    public CityImportService(CityService cityService, Validator validator, ImportOperationService importOpService) {
        this.cityService = cityService;
        this.validator = validator;
        this.importOpService = importOpService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ImportResultDto importCities(List<CityDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            throw new IllegalArgumentException("Файл не содержит записей для импорта.");
        }

        var op = importOpService.start();

        try {
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

            if (!errors.isEmpty()) {
                throw new ImportValidationException(errors);
            }

            List<Long> createdIds = new ArrayList<>(dtos.size());
            for (CityDto dto : dtos) {
                CityDto created = cityService.create(dto);
                createdIds.add(created.getId());
            }

            importOpService.markSuccess(op.getId(), createdIds.size());

            return new ImportResultDto(createdIds.size(), createdIds);
        } catch (RuntimeException ex) {
            importOpService.markFailed(op.getId(), safeMsg(ex));
            throw ex;
        }
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
}
