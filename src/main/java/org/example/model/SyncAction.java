package org.example.model;

import java.time.Instant;

public class SyncAction {
    private ActionType type;
    private String sourcePath;
    private String targetPath;
    private String reason;
    private long timestamp;

    public enum ActionType {
        COPY, DELETE, RENAME, SKIP, CONFLICT
    }

    public SyncAction(ActionType type, String sourcePath, String targetPath, String reason) {
        this.type = type;
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.reason = reason;
        this.timestamp = Instant.now().toEpochMilli();
    }

    // Getters
    public ActionType getType() { return type; }
    public String getSourcePath() { return sourcePath; }
    public String getTargetPath() { return targetPath; }
    public String getReason() { return reason; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("SyncAction{type=%s, source='%s', target='%s', reason='%s'}",
                type, sourcePath, targetPath, reason);
    }
}