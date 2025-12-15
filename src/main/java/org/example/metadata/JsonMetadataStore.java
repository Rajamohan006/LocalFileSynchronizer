package org.example.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JsonMetadataStore implements MetadataStore {
    private static final Logger logger = LoggerFactory.getLogger(JsonMetadataStore.class);
    private final String jsonPath;
    private final Map<String, FileMetadata> metadataMap;
    private final ObjectMapper objectMapper;

    public JsonMetadataStore(String jsonPath) {
        this.jsonPath = jsonPath;
        this.metadataMap = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void initialize() throws IOException {
        File jsonFile = new File(jsonPath);
        File parentDir = jsonFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        if (jsonFile.exists()) {
            // Load existing metadata
            MetadataWrapper wrapper = objectMapper.readValue(jsonFile, MetadataWrapper.class);
            if (wrapper.getMetadata() != null) {
                for (FileMetadata fm : wrapper.getMetadata()) {
                    metadataMap.put(fm.getRelativePath(), fm);
                }
            }
            logger.info("Loaded {} entries from JSON metadata store", metadataMap.size());
        } else {
            // Create empty file
            saveToFile();
            logger.info("Created new JSON metadata store at: {}", jsonPath);
        }
    }

    @Override
    public void save(FileMetadata metadata) throws IOException {
        metadataMap.put(metadata.getRelativePath(), metadata);
        saveToFile();
    }

    @Override
    public FileMetadata get(String relativePath) {
        return metadataMap.get(relativePath);
    }

    @Override
    public List<FileMetadata> getAll() {
        return new ArrayList<>(metadataMap.values());
    }

    @Override
    public void delete(String relativePath) throws IOException {
        metadataMap.remove(relativePath);
        saveToFile();
    }

    @Override
    public void close() {
        try {
            saveToFile();
            logger.info("JSON metadata store closed");
        } catch (IOException e) {
            logger.error("Failed to save metadata on close", e);
        }
    }

    private void saveToFile() throws IOException {
        MetadataWrapper wrapper = new MetadataWrapper();
        wrapper.setMetadata(new ArrayList<>(metadataMap.values()));
        objectMapper.writeValue(new File(jsonPath), wrapper);
    }

    // Wrapper class for JSON structure
    private static class MetadataWrapper {
        private List<FileMetadata> metadata;

        public List<FileMetadata> getMetadata() { return metadata; }
        public void setMetadata(List<FileMetadata> metadata) { this.metadata = metadata; }
    }
}
