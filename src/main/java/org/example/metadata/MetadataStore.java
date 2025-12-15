package org.example.metadata;

import org.example.model.FileMetadata;

import java.io.IOException;
import java.util.List;

public interface MetadataStore {
    void initialize() throws IOException;
    void save(FileMetadata metadata) throws IOException;
    FileMetadata get(String relativePath) throws IOException;
    List<FileMetadata> getAll() throws IOException;
    void delete(String relativePath) throws IOException;
    void close() throws IOException;
}
