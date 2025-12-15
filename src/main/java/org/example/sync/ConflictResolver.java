package org.example.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ConflictResolver {
    private static final Logger logger = LoggerFactory.getLogger(ConflictResolver.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public enum Policy {
        KEEP_SOURCE, KEEP_TARGET, KEEP_BOTH, FAIL
    }

    private final Policy policy;

    public ConflictResolver(Policy policy) {
        this.policy = policy;
    }

    public Path resolveConflict(Path sourcePath, Path targetPath) throws IOException {
        logger.warn("Conflict detected: source={}, target={}", sourcePath, targetPath);

        switch (policy) {
            case KEEP_SOURCE:
                logger.info("Applying KEEP_SOURCE policy - overwriting target");
                return targetPath;

            case KEEP_TARGET:
                logger.info("Applying KEEP_TARGET policy - skipping copy");
                return null;

            case KEEP_BOTH:
                logger.info("Applying KEEP_BOTH policy - creating conflict copy");
                return createConflictCopy(targetPath);

            case FAIL:
                throw new IOException("Conflict resolution policy is FAIL - aborting");

            default:
                throw new IllegalStateException("Unknown conflict policy: " + policy);
        }
    }

    private Path createConflictCopy(Path targetPath) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = targetPath.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');
        String conflictName;

        if (dotIndex > 0) {
            String name = fileName.substring(0, dotIndex);
            String ext = fileName.substring(dotIndex);
            conflictName = name + ".conflict." + timestamp + ext;
        } else {
            conflictName = fileName + ".conflict." + timestamp;
        }

        Path conflictPath = targetPath.getParent().resolve(conflictName);

        if (Files.exists(targetPath)) {
            Files.copy(targetPath, conflictPath);
            logger.info("Created conflict copy: {}", conflictPath);
        }

        return targetPath;
    }
}
