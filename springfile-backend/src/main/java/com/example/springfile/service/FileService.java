package com.example.springfile.service;

import com.example.springfile.dto.FileDto; // Import DTO
import com.example.springfile.model.Category;
import com.example.springfile.model.File;
import com.example.springfile.model.Subcategory;
import com.example.springfile.repository.CategoryRepository;
import com.example.springfile.repository.FileRepository;
import com.example.springfile.repository.SubcategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID; // Import UUID for task IDs
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
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.StreamUtils;
import org.springframework.http.HttpHeaders; // Import HttpHeaders
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Collections; // Import Collections
import com.fasterxml.jackson.databind.JsonNode; // Import JsonNode for handling FastAPI response
import org.springframework.web.server.ResponseStatusException; // For specific exceptions
import org.springframework.http.HttpStatus; // For status codes


@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final FileStorageService fileStorageService;
    private final WebClient fastapiWebClient;
    private final AsyncTaskManager asyncTaskManager; // Added AsyncTaskManager

    // Constants for FastAPI interaction
    private static final String DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String FASTAPI_PREPROCESS_ENDPOINT = "/preprocess/docx/"; // Relative path for preprocessing
    private static final String FASTAPI_EMBEDDING_ENDPOINT = "/embedding/"; // Relative path for embedding
    private static final String FASTAPI_SEARCH_ENDPOINT = "/search/"; // Relative path for search

    @Autowired
    public FileService(FileRepository fileRepository,
                       CategoryRepository categoryRepository,
                       SubcategoryRepository subcategoryRepository,
                       FileStorageService fileStorageService,
                       WebClient fastapiWebClient,
                       AsyncTaskManager asyncTaskManager) { // Added AsyncTaskManager
        this.fileRepository = fileRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.fileStorageService = fileStorageService;
        this.fastapiWebClient = fastapiWebClient;
        this.asyncTaskManager = asyncTaskManager; // Initialize AsyncTaskManager
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
                file.getSubcategory() != null ? file.getSubcategory().getName() : null,
                file.isEmbedding() // Add embedding status
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

    // --- Preprocessing Logic (Now Asynchronous) ---

    /**
     * Initiates the asynchronous preprocessing of DOCX files.
     * @param fileIds List of file IDs to process.
     * @return The unique taskId for tracking this asynchronous operation.
     */
    public String startPreprocessingTask(List<Long> fileIds) {
        String taskId = UUID.randomUUID().toString();
        logger.info("Registering preprocessing task with ID: {} for file IDs: {}", taskId, fileIds);
        asyncTaskManager.registerTask(taskId, "PROCESSING"); // Initial status
        // Call the actual async method
        preprocessFilesAsync(fileIds, taskId);
        return taskId;
    }

    @Async // Mark this method to run asynchronously
    @Transactional // Keep transactional for database operations within the async method
    public void preprocessFilesAsync(List<Long> fileIds, String taskId) {
        Map<Long, String> results = new HashMap<>();
        logger.info("Task {} - Starting async preprocessing for file IDs: {}", taskId, fileIds);
        String finalStatus = "COMPLETED"; // Assume success initially
        String finalMessage = null;

        try {
            for (Long id : fileIds) {
                String fileStatus; // Renamed from 'status' to avoid conflict
            try {
                Optional<File> fileOptional = fileRepository.findById(id);
                if (fileOptional.isEmpty()) {
                    logger.warn("Task {} - Preprocessing skipped: File not found for ID {}", taskId, id);
                    fileStatus = "not_found";
                    results.put(id, fileStatus);
                    continue;
                }

                File file = fileOptional.get();
                String originalFileName = file.getFileName();
                String fileType = file.getFileType();

                // Check if it's a DOCX file
                boolean isDocx = originalFileName != null && originalFileName.toLowerCase().endsWith(".docx");
                // Optional stricter check: && (DOCX_MIME_TYPE.equals(fileType) || fileType == null || fileType.isBlank());

                if (!isDocx) {
                    logger.info("Task {} - Preprocessing skipped: File ID {} ({}) is not a DOCX file.", taskId, id, originalFileName);
                    fileStatus = "not_docx";
                    results.put(id, fileStatus);
                    continue;
                }

                logger.info("Task {} - Preprocessing file ID {} ({}). Loading resource...", taskId, id, originalFileName);
                Resource originalResource = fileStorageService.loadFileAsResource(file.getStorageIdentifier());

                if (!originalResource.exists() || !originalResource.isReadable()) {
                    logger.error("Task {} - Preprocessing failed: Cannot read original file resource for ID {}", taskId, id);
                    fileStatus = "error: cannot read original file";
                    results.put(id, fileStatus);
                    finalStatus = "FAILED"; // Mark overall task as failed if any file fails critically
                    finalMessage = "Failed to read original file for ID " + id;
                    continue; // Continue to next file ID
                }

                // Prepare request for FastAPI
                MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                // Pass the resource directly. WebClient handles streaming.
                // Ensure the filename is passed correctly for FastAPI to read it.
                bodyBuilder.part("file", originalResource).filename(originalFileName);

                logger.info("Task {} - Calling FastAPI to preprocess file ID {}", taskId, id);
                // Call FastAPI service and block for the result (within the loop)
                // Consider if parallel calls are needed for performance with many files.
                Resource processedResource = fastapiWebClient.post()
                        .uri(FASTAPI_PREPROCESS_ENDPOINT)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                        .retrieve()
                        // Handle potential errors from FastAPI
                        .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(), // Renamed lambda param
                                  clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException("FastAPI error: " + clientResponse.statusCode() + " - " + errorBody))))
                        .bodyToMono(Resource.class)
                        .block(); // Blocking here simplifies logic but processes files sequentially.

                if (processedResource != null) {
                    logger.info("Task {} - Received processed resource from FastAPI for file ID {}", taskId, id);
                    // Store the processed content as a NEW file
                    String newStorageIdentifier = fileStorageService.storeFile(processedResource, originalFileName);

                    // Create a new File entity for the processed file
                    File processedFile = new File();
                    String processedFileName = originalFileName.replaceFirst("(?i)\\.docx$", "_processed.docx");
                    if (processedFileName.equals(originalFileName)) { // Handle case where extension wasn't found or name didn't end with .docx
                        processedFileName = originalFileName + "_processed";
                    }
                    processedFile.setFileName(processedFileName);
                    processedFile.setStorageIdentifier(newStorageIdentifier);
                    processedFile.setFileType(DOCX_MIME_TYPE); // Assume it's still DOCX
                    // Try to get size, handle potential exception
                    try {
                        processedFile.setSize(processedResource.contentLength());
                    } catch (IOException e) {
                        logger.warn("Task {} - Could not determine size of processed resource for original file ID {}: {}", taskId, id, e.getMessage());
                        processedFile.setSize(-1L); // Indicate unknown size
                    }
                    processedFile.setCategory(file.getCategory());
                    processedFile.setSubcategory(file.getSubcategory()); // Copy subcategory from original
                    processedFile.setUploadTimestamp(LocalDateTime.now()); // Set new timestamp

                    fileRepository.save(processedFile);

                    logger.info("Task {} - Successfully processed file ID {} and saved as new file with ID {} and storage ID {}",
                            taskId, id, processedFile.getId(), newStorageIdentifier);
                    fileStatus = "processed_new_file_id=" + processedFile.getId();
                } else {
                    logger.error("Task {} - Preprocessing failed: No processed resource received from FastAPI for file ID {}", taskId, id);
                    fileStatus = "error: no processed data received";
                    finalStatus = "FAILED"; // Mark overall task as failed
                    finalMessage = (finalMessage == null ? "" : finalMessage + "; ") + "No processed data for ID " + id;
                }

            } catch (Exception e) {
                logger.error("Task {} - Preprocessing failed for file ID {}: {}", taskId, id, e.getMessage(), e);
                fileStatus = "error: " + e.getMessage();
                finalStatus = "FAILED"; // Mark overall task as failed
                finalMessage = (finalMessage == null ? "" : finalMessage + "; ") + "Error processing ID " + id + ": " + e.getMessage();
            }
            results.put(id, fileStatus);
        } // End of loop through fileIds

        // Update the final task status
        logger.info("Task {} - Async preprocessing finished. Final Status: {}, Results: {}", taskId, finalStatus, results);
        asyncTaskManager.updateTaskStatus(taskId, finalStatus, finalMessage, results);

    } catch (Exception e) {
        // Catch unexpected errors during the async execution
        logger.error("Task {} - Unexpected error during async preprocessing task: {}", taskId, e.getMessage(), e);
        asyncTaskManager.updateTaskStatus(taskId, "FAILED", "Unexpected error: " + e.getMessage(), results);
    }
    }

    // --- Embedding Logic ---

    // Simple record for the FastAPI embedding request payload
    private record EmbeddingRequest(String file_path) {}

    /**
     * Requests embedding generation from the FastAPI service for the given file IDs.
     * Updates the embedding status of the file in the database upon success.
     * Note: This implementation processes files sequentially using the injected WebClient.
     * Consider making it fully asynchronous using reactive chains if performance is critical.
     *
     * @param fileIds List of file IDs to request embedding for.
     */
    @Transactional
    public void requestEmbeddings(List<Long> fileIds) {
        logger.info("Starting embedding request process for file IDs: {}", fileIds);

        for (Long id : fileIds) {
            try {
                Optional<File> fileOptional = fileRepository.findById(id);
                if (fileOptional.isEmpty()) {
                    logger.warn("Embedding request skipped: File not found for ID {}", id);
                    continue; // Skip to the next ID
                }

                File file = fileOptional.get();

                // Removed check for existing embedding status

                String storageIdentifier = file.getStorageIdentifier();
                if (storageIdentifier == null || storageIdentifier.isBlank()) {
                     logger.warn("Embedding request skipped: File ID {} has no valid storage identifier.", id);
                     continue;
                }

                logger.info("Requesting embedding for file ID {} (Path: {})", id, storageIdentifier);

                EmbeddingRequest payload = new EmbeddingRequest(storageIdentifier);

                // Use the injected fastapiWebClient bean
                fastapiWebClient.post()
                        .uri(FASTAPI_EMBEDDING_ENDPOINT) // Use relative path
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(BodyInserters.fromValue(payload))
                        .retrieve()
                        // Check for successful response (e.g., 2xx)
                        .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                                  clientResponse -> clientResponse.bodyToMono(String.class)
                                        .flatMap(errorBody -> {
                                            logger.error("FastAPI embedding error for file ID {}: {} - {}", id, clientResponse.statusCode(), errorBody);
                                            // Create an error specific to this failure
                                            return Mono.error(new RuntimeException("FastAPI embedding call failed for file ID " + id + " with status " + clientResponse.statusCode()));
                                        }))
                        .bodyToMono(Void.class) // We don't need the response body, just success status
                        .block(); // Block for simplicity in this loop

                // If block() completes without error, the call was successful
                logger.info("Successfully received embedding confirmation from FastAPI for file ID {}", id);
                file.setEmbedding(true);
                fileRepository.save(file);
                logger.info("Updated embedding status to true for file ID {}", id);

            } catch (Exception e) {
                // Log error for this specific file but continue processing others
                logger.error("Error during embedding request process for file ID {}: {}", id, e.getMessage(), e);
                // Avoid re-throwing here to allow processing of subsequent IDs
            }
        }
        logger.info("Finished embedding request process for file IDs: {}", fileIds);
    }

    // --- Search Logic ---

    // Simple record for the FastAPI search request payload
    private record SearchRequest(String query, int n_results) {}

    /**
     * Performs a similarity search by calling the FastAPI /search/ endpoint.
     *
     * @param query The search query string.
     * @param nResults The maximum number of results to return.
     * @return JsonNode representing the search results from FastAPI.
     * @throws ResponseStatusException if the FastAPI call fails or returns an error status.
     */
    public JsonNode searchEmbeddings(String query, int nResults) {
        logger.info("Initiating search request to FastAPI. Query: '{}', n_results: {}", query, nResults);

        // Ensure nResults has a sensible default if not provided correctly (e.g., <= 0)
        int effectiveNResults = (nResults > 0) ? nResults : 5; // Default to 5 if invalid

        SearchRequest payload = new SearchRequest(query, effectiveNResults);

        try {
            // Use the injected fastapiWebClient bean
            JsonNode response = fastapiWebClient.post()
                    .uri(FASTAPI_SEARCH_ENDPOINT) // Use relative path for search
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    // Handle non-2xx responses from FastAPI
                    .onStatus(httpStatus -> !httpStatus.is2xxSuccessful(),
                              clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        HttpStatus status = HttpStatus.resolve(clientResponse.statusCode().value());
                                        if (status == null) {
                                            status = HttpStatus.INTERNAL_SERVER_ERROR; // Default if unknown
                                        }
                                        logger.error("FastAPI search error: {} - {}", clientResponse.statusCode(), errorBody);
                                        // Create a specific exception with status code and body
                                        return Mono.error(new ResponseStatusException(status, "FastAPI search failed: " + errorBody));
                                    }))
                    .bodyToMono(JsonNode.class) // Expecting a JSON response
                    .block(); // Block for simplicity

            logger.info("Successfully received search results from FastAPI for query: '{}'", query);
            return response;

        } catch (ResponseStatusException e) {
             // Re-throw exceptions that already have status codes
             logger.error("Search failed due to ResponseStatusException: {}", e.getMessage());
             throw e;
        } catch (Exception e) {
            // Catch other WebClient or unexpected errors
            logger.error("Error during search request to FastAPI for query '{}': {}", query, e.getMessage(), e);
            // Throw a generic internal server error
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to communicate with search service: " + e.getMessage(), e);
        }
    }
}
