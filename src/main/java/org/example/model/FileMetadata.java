package org.example.model;

import java.time.Instant;

public class FileMetadata {
    private String relativePath;
    private long size;
    private long mtime;
    private String checksum;
    private int version;
    private long lastSyncedAt;
    private FileStatus status;

    public enum FileStatus {
        NORMAL, DELETED, CONFLICT
    }

    public FileMetadata() {
        this.version = 1;
        this.status = FileStatus.NORMAL;
        this.lastSyncedAt = Instant.now().toEpochMilli();
    }

    public FileMetadata(String relativePath, long size, long mtime, String checksum) {
        this();
        this.relativePath = relativePath;
        this.size = size;
        this.mtime = mtime;
        this.checksum = checksum;
    }

    // Getters and Setters
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getMtime() { return mtime; }
    public void setMtime(long mtime) { this.mtime = mtime; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(long lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("FileMetadata{path='%s', size=%d, mtime=%d, checksum='%s', version=%d}",
                relativePath, size, mtime, checksum != null ? checksum.substring(0, 8) + "..." : "null", version);
    }
}