package org.example.model;

import java.util.concurrent.atomic.AtomicLong;

public class SyncStats {
    private final AtomicLong filesSynced = new AtomicLong(0);
    private final AtomicLong bytesCopied = new AtomicLong(0);
    private final AtomicLong conflicts = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);
    private final long startTime;

    public SyncStats() {
        this.startTime = System.currentTimeMillis();
    }

    public void incrementFilesSynced() { filesSynced.incrementAndGet(); }
    public void addBytesCopied(long bytes) { bytesCopied.addAndGet(bytes); }
    public void incrementConflicts() { conflicts.incrementAndGet(); }
    public void incrementErrors() { errors.incrementAndGet(); }

    public long getFilesSynced() { return filesSynced.get(); }
    public long getBytesCopied() { return bytesCopied.get(); }
    public long getConflicts() { return conflicts.get(); }
    public long getErrors() { return errors.get(); }
    public long getUptimeSeconds() { return (System.currentTimeMillis() - startTime) / 1000; }

    @Override
    public String toString() {
        return String.format("Stats: Files=%d, Bytes=%d, Conflicts=%d, Errors=%d, Uptime=%ds",
                filesSynced.get(), bytesCopied.get(), conflicts.get(), errors.get(), getUptimeSeconds());
    }
}