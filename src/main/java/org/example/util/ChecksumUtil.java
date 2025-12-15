package org.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class ChecksumUtil {
    private static final Logger logger = LoggerFactory.getLogger(ChecksumUtil.class);
    private static final ConcurrentHashMap<String, String> checksumCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    public static String calculateChecksum(Path filePath, String algorithm, int chunkSize) throws IOException {
        // Check cache first
        String cacheKey = filePath.toString() + ":" + Files.getLastModifiedTime(filePath).toMillis();
        String cached = checksumCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            try (InputStream fis = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[chunkSize];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            String checksum = bytesToHex(hashBytes);

            // Cache the result (with simple size limit)
            if (checksumCache.size() < MAX_CACHE_SIZE) {
                checksumCache.put(cacheKey, checksum);
            }

            return checksum;
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Unsupported checksum algorithm: " + algorithm, e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void clearCache() {
        checksumCache.clear();
    }
}
