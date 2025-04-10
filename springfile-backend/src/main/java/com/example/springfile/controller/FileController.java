package com.example.springfile.controller;

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

import java.util.Map;

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
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam("category_id") Long categoryId,
                                         @RequestParam("subcategory_id") Long subcategoryId) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File cannot be empty"));
        }

        try {
            logger.info("Received file upload request: name={}, size={}, categoryId={}, subcategoryId={}",
                    file.getOriginalFilename(), file.getSize(), categoryId, subcategoryId);

            File savedFile = fileService.uploadFile(file, categoryId, subcategoryId);

            logger.info("File uploaded successfully: id={}, storageIdentifier={}", savedFile.getId(), savedFile.getStorageIdentifier());

            // Return basic info about the saved file
            Map<String, Object> responseBody = Map.of(
                    "message", "File uploaded successfully!",
                    "fileId", savedFile.getId(),
                    "fileName", savedFile.getFileName(),
                    "storageIdentifier", savedFile.getStorageIdentifier()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);

        } catch (RuntimeException e) {
            logger.error("Error uploading file: {}", e.getMessage(), e);
            // Provide a more generic error message to the client
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload the file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred during file upload.", e);
        }
    }
}
