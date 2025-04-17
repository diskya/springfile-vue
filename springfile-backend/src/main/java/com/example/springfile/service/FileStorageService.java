package com.example.springfile.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class); // Add logger

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }

            // Generate a unique file name
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return uniqueFileName; // Return the unique name used for storage
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    /**
     * Stores the content of a Resource as a new file with a unique identifier.
     * The file extension is derived from the provided original filename.
     *
     * @param resource The resource containing the file content.
     * @param originalFileNameForExtension The original filename to derive the extension from.
     * @return The unique storage identifier (filename) generated for the new file.
     */
    public String storeFile(Resource resource, String originalFileNameForExtension) {
        String cleanOriginalName = StringUtils.cleanPath(originalFileNameForExtension);
        String fileExtension = "";
        try {
            // Check for invalid characters in the original name (for extension extraction safety)
            if (cleanOriginalName.contains("..")) {
                throw new RuntimeException("Sorry! Original filename contains invalid path sequence " + cleanOriginalName);
            }

            int dotIndex = cleanOriginalName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = cleanOriginalName.substring(dotIndex);
            }

            // Generate a new unique file name
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Copy resource stream to the target location
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName).normalize();
            logger.debug("Attempting to store new resource at: {}", targetLocation);

            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING); // Use REPLACE_EXISTING for robustness, though UUID collision is unlikely
                logger.info("Successfully stored new file: {}", uniqueFileName);
            }
            return uniqueFileName; // Return the new unique identifier
        } catch (IOException ex) {
            logger.error("Could not store resource {} (derived from {}): {}", resource.getDescription(), cleanOriginalName, ex.getMessage(), ex);
            throw new RuntimeException("Could not store resource " + resource.getDescription() + ". Please try again!", ex);
        }
    }

    public Resource loadFileAsResource(String storageIdentifier) {
        try {
            Path filePath = this.fileStorageLocation.resolve(storageIdentifier).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + storageIdentifier);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found " + storageIdentifier, ex);
        }
    }

    public void deleteFile(String storageIdentifier) {
        try {
            Path targetLocation = this.fileStorageLocation.resolve(storageIdentifier).normalize();
            boolean deleted = Files.deleteIfExists(targetLocation);
            if (deleted) {
                logger.info("Successfully deleted file: {}", targetLocation);
            } else {
                logger.warn("File to delete not found: {}", targetLocation);
                // Depending on requirements, you might throw an exception here
                // throw new RuntimeException("File not found: " + storageIdentifier);
            }
        } catch (IOException ex) {
            logger.error("Could not delete file {}: {}", storageIdentifier, ex.getMessage(), ex);
            // Wrap and rethrow or handle as appropriate for your application's error strategy
            throw new RuntimeException("Could not delete file " + storageIdentifier + ". Please try again!", ex);
        }
    }
}
