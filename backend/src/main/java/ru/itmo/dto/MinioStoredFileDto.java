package ru.itmo.dto;

public class MinioStoredFileDto {
    private String objectKey;
    private String fileName;
    private long sizeBytes;
    private String lastModified;
    private String downloadUrl;

    public MinioStoredFileDto() {}

    public MinioStoredFileDto(String objectKey, String fileName, long sizeBytes, String lastModified) {
        this.objectKey = objectKey;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
    }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}
