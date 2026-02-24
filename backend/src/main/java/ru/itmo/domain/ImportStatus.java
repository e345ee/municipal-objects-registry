package ru.itmo.domain;

public enum ImportStatus {
    IN_PROGRESS,
    FILE_PREPARED,
    FILE_COMMITTED,
    DB_COMMITTED,
    SUCCESS,
    FAILED,
    COMPENSATED
}
