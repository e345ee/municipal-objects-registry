package org.itmo.api;

import java.time.OffsetDateTime;
import java.util.Map;

public class ErrorResponse {

    private String error;
    private String message;
    private int status;
    private String path;
    private OffsetDateTime timestamp;
    private Map<String, Object> details;


    public ErrorResponse() {}

    public ErrorResponse(String error, String message, int status, String path,  Map<String, Object> details) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.path = path;
        this.timestamp = OffsetDateTime.now();
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
