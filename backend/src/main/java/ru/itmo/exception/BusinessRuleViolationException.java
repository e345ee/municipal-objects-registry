package ru.itmo.exception;

public class BusinessRuleViolationException extends RuntimeException {
    private final String code;

    public BusinessRuleViolationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
