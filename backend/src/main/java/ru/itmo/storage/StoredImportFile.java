package ru.itmo.storage;

public class StoredImportFile {
    private final byte[] bytes;
    private final String contentType;

    public StoredImportFile(byte[] bytes, String contentType) {
        this.bytes = bytes;
        this.contentType = (contentType == null || contentType.isBlank()) ? "application/json" : contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getContentType() {
        return contentType;
    }
}
