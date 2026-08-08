package com.docket.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.docket.exception.ApiException;

@Service
public class StorageService {

    private final Path storageDirectory = Paths.get("uploads");

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public StorageService() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE", "Failed to store empty file.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_TYPE", 
                "Only PDF, JPEG, and PNG files are allowed.");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown.ext");
        if (originalFilename.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", 
                "Cannot store file with relative path outside current directory.");
        }

        // UUID prefix to avoid naming collisions
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        try {
            Path destinationFile = this.storageDirectory.resolve(Paths.get(uniqueFileName)).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.storageDirectory.toAbsolutePath())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_FILE_PATH", "Cannot store file outside current directory.");
            }
            
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Return URL path where it can be accessed
            return "/uploads/" + uniqueFileName;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_ERROR", "Failed to store file.");
        }
    }
}
