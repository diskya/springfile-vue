package com.example.springfile.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Storage storage; // Google Cloud Storage client
    private final String bucketName; // Name of the GCS bucket

    @Autowired
    public FileStorageService(Storage storage, @Value("${gcs.bucket.name}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
        logger.info("FileStorageService initialized with GCS bucket: {}", bucketName);
        if (bucketName == null || bucketName.isBlank() || bucketName.equals("${GCS_BUCKET_NAME}")) {
             logger.error("GCS Bucket Name is not configured properly. Check environment variable GCS_BUCKET_NAME.");
             // Optionally throw an exception here to prevent startup if the bucket name isn't configured
             // throw new IllegalStateException("GCS Bucket Name is not configured.");
        }
    }

    // No @PostConstruct needed for GCS, bucket should exist

    /**
     * Stores the uploaded file in Google Cloud Storage.
     *
     * @param file The MultipartFile to store.
     * @return The unique storage identifier (object name in GCS).
     */
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }

            // Generate a unique object name (storage identifier)
            String storageIdentifier = UUID.randomUUID().toString() + fileExtension;

            BlobId blobId = BlobId.of(bucketName, storageIdentifier);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType()) // Store content type
                    .build();

            logger.debug("Attempting to upload file '{}' to GCS bucket '{}' as '{}'", originalFileName, bucketName, storageIdentifier);

            // Upload the file content
            storage.create(blobInfo, file.getBytes());

            logger.info("Successfully uploaded file '{}' to GCS bucket '{}' as '{}'", originalFileName, bucketName, storageIdentifier);
            return storageIdentifier;

        } catch (IOException ex) {
            logger.error("IOException during GCS upload for file {}: {}", originalFileName, ex.getMessage(), ex);
            throw new RuntimeException("Could not store file " + originalFileName + ". IO Error.", ex);
        } catch (StorageException ex) {
            logger.error("StorageException during GCS upload for file {}: {}", originalFileName, ex.getMessage(), ex);
            throw new RuntimeException("Could not store file " + originalFileName + ". Storage Error.", ex);
        } catch (Exception ex) {
            logger.error("Unexpected error during GCS upload for file {}: {}", originalFileName, ex.getMessage(), ex);
            throw new RuntimeException("Could not store file " + originalFileName + ". Unexpected error.", ex);
        }
    }

    /**
     * Stores the content of a Resource as a new object in Google Cloud Storage.
     *
     * @param resource The resource containing the file content.
     * @param originalFileNameForExtension The original filename to derive the extension from.
     * @return The unique storage identifier (object name) generated for the new object.
     */
    public String storeFile(Resource resource, String originalFileNameForExtension) {
        String cleanOriginalName = StringUtils.cleanPath(originalFileNameForExtension);
        String fileExtension = "";
        try {
            if (cleanOriginalName.contains("..")) {
                throw new RuntimeException("Sorry! Original filename contains invalid path sequence " + cleanOriginalName);
            }

            int dotIndex = cleanOriginalName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = cleanOriginalName.substring(dotIndex);
            }

            String storageIdentifier = UUID.randomUUID().toString() + fileExtension;

            BlobId blobId = BlobId.of(bucketName, storageIdentifier);
            // We might not know the content type from the resource easily, set later if needed or use default
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

            logger.debug("Attempting to store resource '{}' to GCS bucket '{}' as '{}'", resource.getDescription(), bucketName, storageIdentifier);

            try (InputStream inputStream = resource.getInputStream()) {
                storage.createFrom(blobInfo, inputStream);
            }

            logger.info("Successfully stored resource '{}' to GCS bucket '{}' as '{}'", resource.getDescription(), bucketName, storageIdentifier);
            return storageIdentifier;

        } catch (IOException ex) {
            logger.error("IOException during GCS store from resource {}: {}", resource.getDescription(), ex.getMessage(), ex);
            throw new RuntimeException("Could not store resource " + resource.getDescription() + ". IO Error.", ex);
        } catch (StorageException ex) {
            logger.error("StorageException during GCS store from resource {}: {}", resource.getDescription(), ex.getMessage(), ex);
            throw new RuntimeException("Could not store resource " + resource.getDescription() + ". Storage Error.", ex);
        } catch (Exception ex) {
            logger.error("Unexpected error during GCS store from resource {}: {}", resource.getDescription(), ex.getMessage(), ex);
            throw new RuntimeException("Could not store resource " + resource.getDescription() + ". Unexpected error.", ex);
        }
    }

    /**
     * Loads a file from Google Cloud Storage as a Resource.
     *
     * @param storageIdentifier The unique object name in GCS.
     * @return A Resource representing the file content.
     */
    public Resource loadFileAsResource(String storageIdentifier) {
        try {
            logger.debug("Attempting to load file from GCS bucket '{}' with identifier '{}'", bucketName, storageIdentifier);
            byte[] content = storage.readAllBytes(BlobId.of(bucketName, storageIdentifier));
            Resource resource = new ByteArrayResource(content);

            if (resource.exists()) {
                 logger.info("Successfully loaded resource from GCS for identifier '{}'", storageIdentifier);
                return resource;
            } else {
                // This case might be less likely with readAllBytes unless the object is empty or deleted race condition
                logger.error("GCS resource not found or empty for identifier '{}' in bucket '{}'", storageIdentifier, bucketName);
                throw new RuntimeException("File not found " + storageIdentifier);
            }
        } catch (StorageException e) {
             // Handle cases where the blob doesn't exist (e.g., 404)
            if (e.getCode() == 404) {
                 logger.error("File not found in GCS bucket '{}' with identifier '{}'", bucketName, storageIdentifier);
                 throw new RuntimeException("File not found " + storageIdentifier, e);
            } else {
                 logger.error("StorageException loading file '{}' from GCS bucket '{}': {}", storageIdentifier, bucketName, e.getMessage(), e);
                 throw new RuntimeException("Could not load file " + storageIdentifier + ". Storage Error.", e);
            }
        } catch (Exception e) {
            logger.error("Unexpected error loading file '{}' from GCS bucket '{}': {}", storageIdentifier, bucketName, e.getMessage(), e);
            throw new RuntimeException("Could not load file " + storageIdentifier + ". Unexpected error.", e);
        }
    }

    /**
     * Deletes a file from Google Cloud Storage.
     *
     * @param storageIdentifier The unique object name in GCS.
     */
    public void deleteFile(String storageIdentifier) {
        try {
            BlobId blobId = BlobId.of(bucketName, storageIdentifier);
            logger.debug("Attempting to delete file from GCS bucket '{}' with identifier '{}'", bucketName, storageIdentifier);
            boolean deleted = storage.delete(blobId);
            if (deleted) {
                logger.info("Successfully deleted file from GCS: {}", storageIdentifier);
            } else {
                // This means the file didn't exist at the time of deletion attempt
                logger.warn("File to delete not found in GCS bucket '{}': {}", bucketName, storageIdentifier);
                // Optionally throw an exception if non-existence is an error condition
                // throw new RuntimeException("File not found: " + storageIdentifier);
            }
        } catch (StorageException e) {
            logger.error("StorageException deleting file '{}' from GCS bucket '{}': {}", storageIdentifier, bucketName, e.getMessage(), e);
            throw new RuntimeException("Could not delete file " + storageIdentifier + ". Storage Error.", e);
        } catch (Exception e) {
             logger.error("Unexpected error deleting file '{}' from GCS bucket '{}': {}", storageIdentifier, bucketName, e.getMessage(), e);
            throw new RuntimeException("Could not delete file " + storageIdentifier + ". Unexpected error.", e);
        }
    }
}
