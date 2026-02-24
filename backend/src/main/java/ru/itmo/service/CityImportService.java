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
import ru.itmo.storage.ImportFileStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CityImportService {

    public static final String DEBUG_FAIL_AFTER_FILE_COMMIT = "AFTER_FILE_COMMIT";

    private final CityService cityService;
    private final Validator validator;
    private final ImportOperationService importOpService;
    private final PlatformTransactionManager txManager;
    private final ImportFileStorageService fileStorageService;
    private final InfraFailureSimulationService infraFailures;

    public CityImportService(CityService cityService,
                             Validator validator,
                             ImportOperationService importOpService,
                             PlatformTransactionManager txManager,
                             ImportFileStorageService fileStorageService,
                             InfraFailureSimulationService infraFailures) {
        this.cityService = cityService;
        this.validator = validator;
        this.importOpService = importOpService;
        this.txManager = txManager;
        this.fileStorageService = fileStorageService;
        this.infraFailures = infraFailures;
    }

    public ImportResultDto importCities(List<CityDto> dtos, byte[] fileBytes, String originalFilename, String debugFailStage) {
        if (dtos == null || dtos.isEmpty()) {
            throw new IllegalArgumentException("Файл не содержит записей для импорта.");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Файл не передан или пустой.");
        }

        String txId = UUID.randomUUID().toString();
        var op = importOpService.start(txId, originalFilename, fileStorageService.getBucketName());

        String stagingKey = null;
        String finalKey = null;

        try {
            List<ImportValidationException.ItemError> errors = validateAll(dtos);
            if (!errors.isEmpty()) {
                throw new ImportValidationException(errors);
            }

            // Phase 1: PREPARE (upload file into staging area)
            stagingKey = fileStorageService.prepare(txId, fileBytes, originalFilename);
            importOpService.markFilePrepared(op.getId(), stagingKey);

            // Phase 2a: COMMIT file in storage (staging -> final)
            finalKey = fileStorageService.commitPrepared(txId, stagingKey, originalFilename);
            stagingKey = null;
            importOpService.markFileCommitted(op.getId(), finalKey);

            maybeFailBetweenResources(debugFailStage);

            // Phase 2b: COMMIT DB changes in a single SERIALIZABLE transaction
            infraFailures.assertPostgresAvailable();
            ImportResultDto res = runImportInSerializableTxWithRetry(dtos);
            importOpService.markDbCommitted(op.getId());
            importOpService.markSuccess(op.getId(), res.getCreated());
            return res;
        } catch (RuntimeException ex) {
            boolean hadStorageArtifact = stagingKey != null || finalKey != null;
            boolean compensated = compensateStorageQuietly(stagingKey, finalKey);

            String message = safeMsg(ex);
            if (hadStorageArtifact && compensated) {
                importOpService.markCompensated(op.getId(), message);
            } else {
                importOpService.markFailed(op.getId(), message);
            }
            throw ex;
        }
    }

    private void maybeFailBetweenResources(String debugFailStage) {
        if (debugFailStage == null) return;
        if (DEBUG_FAIL_AFTER_FILE_COMMIT.equalsIgnoreCase(debugFailStage.trim())) {
            throw new RuntimeException("Demo failpoint: RuntimeException after file commit and before DB commit");
        }
    }

    private boolean compensateStorageQuietly(String stagingKey, String finalKey) {
        boolean ok = true;
        if (finalKey != null) {
            try {
                fileStorageService.rollbackCommittedQuietly(finalKey);
            } catch (RuntimeException ignored) {
                ok = false;
            }
        }
        if (stagingKey != null) {
            try {
                fileStorageService.rollbackPreparedQuietly(stagingKey);
            } catch (RuntimeException ignored) {
                ok = false;
            }
        }
        return ok;
    }

    private ImportResultDto runImportInSerializableTxWithRetry(List<CityDto> dtos) {
        int maxAttempts = 3;
        long backoffMs = 30;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return runSerializable(() -> doImportTransactional(dtos));
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

                throw ex;
            }
        }

        throw new IllegalStateException("Unreachable code");
    }

    private ImportResultDto doImportTransactional(List<CityDto> dtos) {
        List<Long> createdIds = new ArrayList<>(dtos.size());

        for (CityDto dto : dtos) {
            CityDto created = cityService.createInCurrentTransactionForImport(dto);
            createdIds.add(created.getId());
        }

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

    private String safeMsg(Throwable ex) {
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
