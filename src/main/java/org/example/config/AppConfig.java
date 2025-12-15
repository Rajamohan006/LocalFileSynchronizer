package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class AppConfig {
    private PeerConfig peer;
    private List<String> sourceDirs;
    private String targetDir;
    private SyncConfig sync;
    private String conflictPolicy;
    private List<String> excludePatterns;
    private MetadataConfig metadata;
    private LoggingConfig logging;

    // NEW: Peer configuration class - THIS WAS MISSING!
    public static class PeerConfig {
        private String name;
        private int port;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    // Existing nested classes
    public static class SyncConfig {
        private String checksumAlgorithm;
        private int chunkSize;
        private int threadPoolSize;
        private int scanIntervalSeconds;

        public String getChecksumAlgorithm() { return checksumAlgorithm; }
        public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public int getScanIntervalSeconds() { return scanIntervalSeconds; }
        public void setScanIntervalSeconds(int scanIntervalSeconds) { this.scanIntervalSeconds = scanIntervalSeconds; }
    }

    public static class MetadataConfig {
        private String type;
        private String dbPath;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDbPath() { return dbPath; }
        public void setDbPath(String dbPath) { this.dbPath = dbPath; }
    }

    public static class LoggingConfig {
        private String level;
        private String logFile;
        private String maxFileSize;
        private int maxHistory;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getLogFile() { return logFile; }
        public void setLogFile(String logFile) { this.logFile = logFile; }
        public String getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(String maxFileSize) { this.maxFileSize = maxFileSize; }
        public int getMaxHistory() { return maxHistory; }
        public void setMaxHistory(int maxHistory) { this.maxHistory = maxHistory; }
    }

    // Getters and setters - INCLUDING getPeer() which was missing!
    public PeerConfig getPeer() { return peer; }
    public void setPeer(PeerConfig peer) { this.peer = peer; }

    public List<String> getSourceDirs() { return sourceDirs; }
    public void setSourceDirs(List<String> sourceDirs) { this.sourceDirs = sourceDirs; }

    public String getTargetDir() { return targetDir; }
    public void setTargetDir(String targetDir) { this.targetDir = targetDir; }

    public SyncConfig getSync() { return sync; }
    public void setSync(SyncConfig sync) { this.sync = sync; }

    public String getConflictPolicy() { return conflictPolicy; }
    public void setConflictPolicy(String conflictPolicy) { this.conflictPolicy = conflictPolicy; }

    public List<String> getExcludePatterns() { return excludePatterns; }
    public void setExcludePatterns(List<String> excludePatterns) { this.excludePatterns = excludePatterns; }

    public MetadataConfig getMetadata() { return metadata; }
    public void setMetadata(MetadataConfig metadata) { this.metadata = metadata; }

    public LoggingConfig getLogging() { return logging; }
    public void setLogging(LoggingConfig logging) { this.logging = logging; }

    // Load config from YAML file
    public static AppConfig loadFromFile(String configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readValue(new File(configPath), AppConfig.class);
    }

    public void validate() throws IllegalArgumentException {
        if (peer == null || peer.getName() == null || peer.getName().isEmpty()) {
            throw new IllegalArgumentException("Peer configuration is required");
        }
        if (peer.getPort() <= 0 || peer.getPort() > 65535) {
            throw new IllegalArgumentException("Invalid peer port: " + peer.getPort());
        }
        if (sourceDirs == null || sourceDirs.isEmpty()) {
            throw new IllegalArgumentException("Source directories cannot be empty");
        }
        if (targetDir == null || targetDir.isEmpty()) {
            throw new IllegalArgumentException("Target directory cannot be empty");
        }
        if (sync == null || sync.getChunkSize() <= 0) {
            throw new IllegalArgumentException("Invalid sync configuration");
        }
    }
}
