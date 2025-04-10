package com.example.springfile.controller;

import com.example.springfile.dto.FileDto; // Import DTO
import com.example.springfile.model.File;
import com.example.springfile.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;


@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    @Autowired
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestParam("files") MultipartFile[] files, // Changed to accept array
                                          @RequestParam("category_id") Long categoryId,
                                          @RequestParam(value = "subcategory_id", required = false) Long subcategoryId) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Files cannot be empty"));
        }

        List<Map<String, Object>> results = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                logger.warn("Skipping empty file during multi-upload.");
                errors.add("An empty file was provided and skipped.");
                continue; // Skip empty files
            }

            try {
                logger.info("Processing file upload: name={}, size={}, categoryId={}, subcategoryId={}",
                        file.getOriginalFilename(), file.getSize(), categoryId, subcategoryId);

                File savedFile = fileService.uploadFile(file, categoryId, subcategoryId);

                logger.info("File uploaded successfully: id={}, storageIdentifier={}", savedFile.getId(), savedFile.getStorageIdentifier());

                // Collect basic info about the saved file
                Map<String, Object> fileResult = Map.of(
                        "fileId", savedFile.getId(),
                        "fileName", savedFile.getFileName(),
                        "storageIdentifier", savedFile.getStorageIdentifier(),
                        "status", "uploaded"
                );
                results.add(fileResult);

            } catch (RuntimeException e) {
                logger.error("Error uploading file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
                errors.add("Could not upload file '" + file.getOriginalFilename() + "': " + e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error uploading file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
                errors.add("Unexpected error uploading file '" + file.getOriginalFilename() + "'.");
            }
        }

        Map<String, Object> responseBody = new java.util.HashMap<>();
        responseBody.put("message", "File upload process completed.");
        responseBody.put("uploadedFiles", results);
        if (!errors.isEmpty()) {
            responseBody.put("errors", errors);
        }

        // Determine overall status code
        HttpStatus status = results.isEmpty() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.CREATED;
        if (!results.isEmpty() && !errors.isEmpty()) {
            status = HttpStatus.MULTI_STATUS; // Indicate partial success
        }

        return ResponseEntity.status(status).body(responseBody);
    }


    @GetMapping
    public ResponseEntity<List<FileDto>> getAllFiles() { // Changed return type
        try {
            List<FileDto> files = fileService.getAllFiles(); // Service now returns DTOs
            if (files.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error retrieving files: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not retrieve files.", e);
        }
    }

    @DeleteMapping("/delete") // Changed endpoint
    public ResponseEntity<?> deleteFiles(@RequestBody List<Long> fileIds) { // Changed method name
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File IDs cannot be empty"));
        }
        logger.info("Received request to delete files with IDs: {}", fileIds); // Updated log message

        try {
            // Call the correctly named service method
            Map<Long, String> deletionResults = fileService.deleteFiles(fileIds); // Corrected service call

            // Separate successes and failures
            List<Long> successes = deletionResults.entrySet().stream()
                    .filter(entry -> "deleted".equals(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<Map<String, Object>> failures = deletionResults.entrySet().stream()
                    .filter(entry -> !"deleted".equals(entry.getValue()))
                    .map(entry -> Map.<String, Object>of("fileId", entry.getKey(), "error", entry.getValue()))
                    .collect(Collectors.toList());

            Map<String, Object> responseBody = new java.util.HashMap<>();
            responseBody.put("message", "File deletion process completed.");
            responseBody.put("deletedFileIds", successes);
            if (!failures.isEmpty()) {
                responseBody.put("errors", failures);
            }

            HttpStatus status = HttpStatus.OK;
            if (!failures.isEmpty() && successes.isEmpty()) {
                status = HttpStatus.INTERNAL_SERVER_ERROR; // All failed
            } else if (!failures.isEmpty()) {
                status = HttpStatus.MULTI_STATUS; // Partial success
            }

            logger.info("Deletion process completed. Successes: {}, Failures: {}", successes.size(), failures.size());
            return ResponseEntity.status(status).body(responseBody);

        } catch (Exception e) {
            logger.error("Unexpected error during file deletion for IDs {}: {}", fileIds, e.getMessage(), e); // Updated log message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred during the deletion process."));
        }
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        try {
            logger.info("Received request to download file with ID: {}", fileId);
            Map<String, Object> fileData = fileService.getFileForDownload(fileId);
            Resource resource = (Resource) fileData.get("resource");
            String originalFileName = (String) fileData.get("fileName");
            String contentType = (String) fileData.get("fileType");

            // Fallback content type if not available
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
                logger.warn("Content type not found for file ID: {}. Using default: {}", fileId, contentType);
            }

            // Encode filename for header
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            logger.info("Prepared file '{}' (type: {}) for download.", originalFileName, contentType);

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName) // Use RFC 5987 format for encoding
                    .body(resource);

        } catch (RuntimeException e) {
            // Handle file not found specifically
            if (e.getMessage().startsWith("File not found")) {
                 logger.error("File not found error for ID {}: {}", fileId, e.getMessage());
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            // Handle other potential errors from service layer
            logger.error("Error preparing file download for ID {}: {}", fileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not download the file.", e);
        } catch (Exception e) {
            // Catch unexpected errors
            logger.error("Unexpected error during file download for ID {}: {}", fileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", e);
        }
    }

    @PostMapping("/download/batch") // Use POST since we send a body
    public ResponseEntity<Resource> downloadFilesAsZip(@RequestBody List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            logger.warn("Received empty file ID list for batch download.");
            return ResponseEntity.badRequest().body(null); // Or build a specific error resource/message
        }
        logger.info("Received request to download files as ZIP with IDs: {}", fileIds);

        try {
            // This service method will need to be created.
            Resource zipResource = fileService.createZipArchiveForFiles(fileIds);

            // Generate filename: springfile-zip-{yyyyMMdd}-{random}.zip
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000); // 4-digit random number
            String zipFileName = String.format("springfile-zip-%s-%d.zip", datePart, randomPart);

            logger.info("Prepared ZIP archive '{}' for download.", zipFileName);

            // Encode filename for header
            String encodedZipFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM) // Standard ZIP MIME type
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedZipFileName)
                    .body(zipResource);

        } catch (RuntimeException e) {
             logger.error("Error creating ZIP archive for file IDs {}: {}", fileIds, e.getMessage(), e);
             // Consider different status codes based on the error (e.g., NOT_FOUND if some files are missing)
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create ZIP archive.", e);
        } catch (Exception e) {
            logger.error("Unexpected error during ZIP archive creation for file IDs {}: {}", fileIds, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during ZIP creation.", e);
        }
    }
}
