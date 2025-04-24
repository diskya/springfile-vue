package com.example.springfile.controller;

import com.example.springfile.dto.FileDto;
import com.example.springfile.dto.SearchQueryDto; // Import DTO for search query
import com.fasterxml.jackson.databind.JsonNode; // To handle generic JSON response from FastAPI
import com.example.springfile.model.File;
import com.example.springfile.service.AsyncTaskManager; // Import AsyncTaskManager
import com.example.springfile.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID; // Import UUID (though task ID generation moved to service)
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
    private final AsyncTaskManager asyncTaskManager; // Inject AsyncTaskManager

    @Autowired
    public FileController(FileService fileService, AsyncTaskManager asyncTaskManager) { // Add to constructor
        this.fileService = fileService;
        this.asyncTaskManager = asyncTaskManager; // Initialize
    }

    // --- Task Status Endpoint ---

    @GetMapping("/process/status/{taskId}") // Changed path to match frontend expectation
    public ResponseEntity<?> getTaskStatus(@PathVariable String taskId) {
        logger.debug("Received request for task status: {}", taskId);
        AsyncTaskManager.TaskStatus status = asyncTaskManager.getTaskStatus(taskId);

        if (status != null) {
            logger.debug("Returning status for task {}: {}", taskId, status.getStatus());
            // Return the whole status object which might contain status, message, results
            return ResponseEntity.ok(status);
        } else {
            logger.warn("Status not found for task ID: {}", taskId);
            // Return 404 Not Found if the task ID is unknown
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Task not found or already cleaned up."));
        }
    }


    // --- Existing Endpoints ---

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

    @GetMapping("/view/{fileId}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId) {
        try {
            logger.info("Received request to view file with ID: {}", fileId);
            Map<String, Object> fileData = fileService.getFileForDownload(fileId); // Re-use existing service method
            Resource resource = (Resource) fileData.get("resource");
            String originalFileName = (String) fileData.get("fileName"); // Keep original name for context, though not strictly needed for inline
            String contentType = (String) fileData.get("fileType");

            // Fallback content type if not available
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream"; // Browser might still download if it doesn't know how to display
                logger.warn("Content type not found for file ID: {}. Using default: {}", fileId, contentType);
            }

            logger.info("Prepared file '{}' (type: {}) for inline viewing.", originalFileName, contentType);

            // Key difference: Content-Disposition is "inline"
            // Encode filename using RFC 5987 format for proper Unicode handling
            String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFileName)
                    .body(resource);

        } catch (RuntimeException e) {
            // Handle file not found specifically
            if (e.getMessage().startsWith("File not found")) {
                 logger.error("File not found error for view request ID {}: {}", fileId, e.getMessage());
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
            }
            // Handle other potential errors from service layer
            logger.error("Error preparing file view for ID {}: {}", fileId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not view the file.", e);
        } catch (Exception e) {
            // Catch unexpected errors
            logger.error("Unexpected error during file view for ID {}: {}", fileId, e.getMessage(), e);
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

    @PostMapping("/process/docx")
    public ResponseEntity<?> processDocxFilesAsync(@RequestBody List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            logger.warn("Received empty file ID list for async DOCX processing.");
            return ResponseEntity.badRequest().body(Map.of("message", "File IDs cannot be empty"));
        }
        logger.info("Received request to start async processing for DOCX files with IDs: {}", fileIds);

        try {
            // Start the async task and get the task ID
            String taskId = fileService.startPreprocessingTask(fileIds);
            logger.info("Started async DOCX processing task with ID: {}", taskId);

            // Return 202 Accepted with the task ID
            return ResponseEntity.accepted().body(Map.of("taskId", taskId));

        } catch (Exception e) {
            // Handle exceptions during the *initiation* of the task
            logger.error("Failed to initiate async DOCX processing for IDs {}: {}", fileIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to start the processing task: " + e.getMessage()));
        }
    }

    // --- New Embedding Endpoint ---

    @PostMapping("/embed")
    public ResponseEntity<?> triggerEmbedding(@RequestBody List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            logger.warn("Received empty file ID list for embedding request.");
            return ResponseEntity.badRequest().body(Map.of("message", "File IDs cannot be empty"));
        }
        logger.info("Received request to trigger embedding for file IDs: {}", fileIds);

        try {
            // Call the service method to request embeddings
            // Note: This service method currently runs synchronously in the loop.
            // Consider making the service method @Async for long-running tasks.
            fileService.requestEmbeddings(fileIds);

            logger.info("Embedding request process initiated successfully for file IDs: {}", fileIds);
            // Return 200 OK indicating the process was initiated
            // The actual embedding happens in the background (if service is async) or sequentially here.
            return ResponseEntity.ok().body(Map.of("message", "Embedding process initiated for " + fileIds.size() + " files."));

        } catch (Exception e) {
            // Handle exceptions during the initiation or execution (if synchronous)
            logger.error("Failed to process embedding request for IDs {}: {}", fileIds, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to process the embedding request: " + e.getMessage()));
        }
    }

    // --- New Search Endpoint ---

    @PostMapping("/search")
    public ResponseEntity<?> searchEmbeddings(@RequestBody SearchQueryDto searchQuery) {
        if (searchQuery == null || searchQuery.getQuery() == null || searchQuery.getQuery().isBlank()) {
            logger.warn("Received empty or invalid search query.");
            return ResponseEntity.badRequest().body(Map.of("message", "Search query cannot be empty"));
        }
        // Optional: Validate n_results if you want to enforce limits here too
        // int nResults = searchQuery.getNResults() > 0 ? searchQuery.getNResults() : 5; // Use default if not provided or invalid

        logger.info("Received search request with query: '{}', n_results: {}", searchQuery.getQuery(), searchQuery.getNResults());

        try {
            // Call a new service method to perform the search via FastAPI
            // The service method should return the response body from FastAPI, likely as JsonNode or a specific DTO
            JsonNode searchResults = fileService.searchEmbeddings(searchQuery.getQuery(), searchQuery.getNResults());

            logger.info("Search completed successfully.");
            // Return the results obtained from the service (which came from FastAPI)
            return ResponseEntity.ok(searchResults);

        } catch (ResponseStatusException e) {
            // Re-throw exceptions that already have status codes (e.g., from service layer)
             logger.error("Search failed with status {}: {}", e.getStatusCode(), e.getReason(), e);
             throw e;
        } catch (Exception e) {
            // Handle other unexpected exceptions
            logger.error("An unexpected error occurred during search for query '{}': {}", searchQuery.getQuery(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An unexpected error occurred during the search: " + e.getMessage()));
        }
    }
}
