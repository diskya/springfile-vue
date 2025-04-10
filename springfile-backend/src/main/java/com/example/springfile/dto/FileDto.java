package com.example.springfile.dto;

import java.time.LocalDateTime;

// DTO to transfer file information to the frontend
public class FileDto {

    private Long id;
    private String fileName;
    private String fileType;
    private long size;
    private LocalDateTime uploadTimestamp;
    private String categoryName; // Flattened data
    private String subcategoryName; // Flattened data

    // Constructors
    public FileDto() {
    }

    public FileDto(Long id, String fileName, String fileType, long size, LocalDateTime uploadTimestamp, String categoryName, String subcategoryName) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.size = size;
        this.uploadTimestamp = uploadTimestamp;
        this.categoryName = categoryName;
        this.subcategoryName = subcategoryName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getSubcategoryName() {
        return subcategoryName;
    }

    public void setSubcategoryName(String subcategoryName) {
        this.subcategoryName = subcategoryName;
    }
}
