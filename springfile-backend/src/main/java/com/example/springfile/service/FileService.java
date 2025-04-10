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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors; // Import Collectors

@Service
public class FileService {

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
    public File uploadFile(MultipartFile multipartFile, Long categoryId, Long subcategoryId) {
        // 1. Validate Category and Subcategory
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + categoryId));

        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new RuntimeException("Subcategory not found with id: " + subcategoryId));

        // Ensure the subcategory belongs to the specified category
        if (!subcategory.getCategory().getId().equals(categoryId)) {
            throw new RuntimeException("Subcategory with id " + subcategoryId +
                                       " does not belong to Category with id " + categoryId);
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
        file.setSubcategory(subcategory);
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

    // Optional: Add methods for retrieving/deleting files later
}
