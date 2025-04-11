package com.example.springfile.service;

import com.example.springfile.dto.FileDto; // Import DTO
import com.example.springfile.model.Category;
import com.example.springfile.model.File;
import com.example.springfile.model.Subcategory;
import com.example.springfile.repository.CategoryRepository;
import com.example.springfile.repository.FileRepository;
import com.example.springfile.repository.SubcategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.StreamUtils;


@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class); // Add logger

    private final FileRepository fileRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    public FileService(FileRepository fileRepository,
                       CategoryRepository categoryRepository,
                       SubcategoryRepository subcategoryRepository,
                       FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    // Change subcategoryId to Long to allow null
    public File uploadFile(MultipartFile multipartFile, Long categoryId, Long subcategoryId) {
        // 1. Validate Category
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        Subcategory subcategory = null; // Initialize as null
        // 2. Validate Subcategory only if ID is provided
        if (subcategoryId != null) {
            subcategory = subcategoryRepository.findById(subcategoryId)
                    .orElseThrow(() -> new RuntimeException("Subcategory not found with id: " + subcategoryId));

            // Ensure the subcategory belongs to the specified category
            if (!subcategory.getCategory().getId().equals(categoryId)) {
                throw new RuntimeException("Subcategory with id " + subcategoryId +
                                           " does not belong to Category with id " + categoryId);
            }
        }

        // 2. Store the file using FileStorageService
        String storageIdentifier = fileStorageService.storeFile(multipartFile);

        // 3. Create and save the File entity
        String originalFileName = StringUtils.cleanPath(multipartFile.getOriginalFilename());
        File file = new File();
        file.setFileName(originalFileName);
        file.setFileType(multipartFile.getContentType());
        file.setSize(multipartFile.getSize());
        file.setStorageIdentifier(storageIdentifier);
        file.setCategory(category);
        if (subcategory != null) { // Set subcategory only if it exists
            file.setSubcategory(subcategory);
        }
        file.setUploadTimestamp(LocalDateTime.now()); // Set timestamp explicitly or rely on constructor

        return fileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public List<FileDto> getAllFiles() {
        List<File> files = fileRepository.findAll();
        return files.stream()
                .map(this::mapToFileDto) // Use a helper method for mapping
                .collect(Collectors.toList());
    }

    // Helper method to map File entity to FileDto
    private FileDto mapToFileDto(File file) {
        return new FileDto(
                file.getId(),
                file.getFileName(),
                file.getFileType(),
                file.getSize(),
                file.getUploadTimestamp(),
                file.getCategory() != null ? file.getCategory().getName() : null, // Safely get names
                file.getSubcategory() != null ? file.getSubcategory().getName() : null
        );
    }

    @Transactional
    public Map<Long, String> deleteFiles(List<Long> fileIds) {
        Map<Long, String> results = new HashMap<>();
        for (Long id : fileIds) {
            try {
                Optional<File> fileOptional = fileRepository.findById(id);
                if (fileOptional.isPresent()) {
                    File file = fileOptional.get();
                    String storageIdentifier = file.getStorageIdentifier();

                    // 1. Attempt to delete physical file
                    try {
                        fileStorageService.deleteFile(storageIdentifier);
                        logger.info("Successfully deleted physical file with storage identifier: {}", storageIdentifier);
                    } catch (Exception e) {
                        // Log the error but proceed to delete DB record if desired,
                        // or handle differently (e.g., mark as orphaned)
                        logger.error("Failed to delete physical file with storage identifier {}: {}", storageIdentifier, e.getMessage(), e);
                        // Depending on requirements, you might want to stop here or just log
                        // results.put(id, "Failed to delete physical file: " + e.getMessage());
                        // continue; // Skip DB deletion if physical deletion fails? Decide based on requirements.
                    }

                    // 2. Delete database record
                    fileRepository.deleteById(id);
                    logger.info("Successfully deleted database record for file ID: {}", id);
                    results.put(id, "deleted");

                } else {
                    logger.warn("File not found in database with ID: {}", id);
                    results.put(id, "File not found in database");
                }
            } catch (Exception e) {
                logger.error("Error deleting file with ID {}: {}", id, e.getMessage(), e);
                results.put(id, "Error during deletion: " + e.getMessage());
            }
        }
        return results;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFileForDownload(Long fileId) {
        logger.debug("Attempting to retrieve file for download with ID: {}", fileId);
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> {
                    logger.warn("File not found for download with ID: {}", fileId);
                    return new RuntimeException("File not found with id: " + fileId);
                });

        logger.debug("Found file record: {}", file.getFileName());
        Resource resource = fileStorageService.loadFileAsResource(file.getStorageIdentifier());
        logger.info("Loaded file resource for ID: {}", fileId);

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("resource", resource);
        fileData.put("fileName", file.getFileName());
        fileData.put("fileType", file.getFileType()); // Include file type
        return fileData;
    }

    @Transactional(readOnly = true) // Read-only as we are just reading files
    public Resource createZipArchiveForFiles(List<Long> fileIds) {
        logger.info("Starting creation of ZIP archive for file IDs: {}", fileIds);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            byte[] buffer = new byte[1024]; // Buffer for copying data

            for (Long fileId : fileIds) {
                try {
                    Optional<File> fileOptional = fileRepository.findById(fileId);
                    if (fileOptional.isPresent()) {
                        File file = fileOptional.get();
                        logger.debug("Adding file to ZIP: ID={}, Name={}", fileId, file.getFileName());
                        Resource resource = fileStorageService.loadFileAsResource(file.getStorageIdentifier());

                        if (resource.exists() && resource.isReadable()) {
                            ZipEntry zipEntry = new ZipEntry(file.getFileName()); // Use original filename
                            zos.putNextEntry(zipEntry);

                            try (InputStream inputStream = resource.getInputStream()) {
                                StreamUtils.copy(inputStream, zos); // Efficiently copy stream
                            }
                            zos.closeEntry();
                            logger.debug("Successfully added file to ZIP: {}", file.getFileName());
                        } else {
                            logger.warn("Resource not found or not readable for file ID: {}, Name: {}", fileId, file.getFileName());
                            // Optionally, add a placeholder entry or skip
                        }
                    } else {
                        logger.warn("File record not found in database for ID: {}. Skipping.", fileId);
                        // Optionally, add a note in the ZIP or just skip
                    }
                } catch (Exception e) {
                    // Log error for this specific file but continue with others
                    logger.error("Error processing file ID {} for ZIP archive: {}", fileId, e.getMessage(), e);
                    // Optionally, add an error marker entry in the ZIP
                }
            }
            // No need to explicitly finish zos here, try-with-resources handles it

        } catch (IOException e) {
            logger.error("IOException during ZIP archive creation for file IDs {}: {}", fileIds, e.getMessage(), e);
            // Throw a runtime exception to indicate failure
            throw new RuntimeException("Failed to create ZIP archive due to IO error", e);
        } catch (Exception e) {
             logger.error("Unexpected error during ZIP archive creation for file IDs {}: {}", fileIds, e.getMessage(), e);
             throw new RuntimeException("Unexpected error creating ZIP archive", e);
        }

        logger.info("Successfully created ZIP archive in memory for file IDs: {}", fileIds);
        return new ByteArrayResource(baos.toByteArray());
    }
}
