package com.docket.service;

import java.io.File;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for persisting and retrieving uploaded documents.
 * Implementations support local disk storage (dev) and S3-compatible object storage (prod).
 */
public interface StorageService {

    /**
     * Persists an uploaded multipart file and returns its storage URL/path.
     *
     * @param file the uploaded MultipartFile
     * @return the unique storage URL / path reference
     */
    String store(MultipartFile file);

    /**
     * Resolves a stored file URL to a java.io.File on disk if available.
     *
     * @param fileUrl the storage URL / path
     * @return the existing File, or null if not found/supported
     */
    File getFile(String fileUrl);

    /**
     * Resolves a stored file URL to a Spring Resource for streaming.
     *
     * @param fileUrl the storage URL / path
     * @return the Spring Resource for the file
     */
    Resource getResource(String fileUrl);

    /**
     * Deletes the stored file from storage if present.
     *
     * @param fileUrl the storage URL / path
     */
    void deleteFile(String fileUrl);
}
