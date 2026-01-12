package ru.itmo.exception;

import java.util.List;

public class ImportValidationException extends RuntimeException {

    private final List<ItemError> errors;

    public ImportValidationException(List<ItemError> errors) {
        super("Import validation failed");
        this.errors = errors;
    }

    public List<ItemError> getErrors() {
        return errors;
    }

    public static class ItemError {
        private int index;
        private String field;
        private String message;

        public ItemError() {}

        public ItemError(int index, String field, String message) {
            this.index = index;
            this.field = field;
            this.message = message;
        }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
