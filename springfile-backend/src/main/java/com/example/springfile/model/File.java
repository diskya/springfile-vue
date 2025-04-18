package com.example.springfile.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType; // MIME type

    @Column(nullable = false)
    private long size; // Size in bytes

    @Column(nullable = false, unique = true)
    private String storageIdentifier; // Path or key for storage location

    @Column(nullable = false)
    private LocalDateTime uploadTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    // Explicitly define column as nullable in DDL
    @JoinColumn(name = "subcategory_id", nullable = true, columnDefinition = "BIGINT NULL")
    private Subcategory subcategory;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean embedding = false; // Default to false

    // Constructors
    public File() {
        this.uploadTimestamp = LocalDateTime.now();
    }

    public File(String fileName, String fileType, long size, String storageIdentifier, Category category, Subcategory subcategory) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.size = size;
        this.storageIdentifier = storageIdentifier;
        this.category = category;
        this.subcategory = subcategory;
        this.uploadTimestamp = LocalDateTime.now();
        this.embedding = false; // Initialize in constructor
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

    public String getStorageIdentifier() {
        return storageIdentifier;
    }

    public void setStorageIdentifier(String storageIdentifier) {
        this.storageIdentifier = storageIdentifier;
    }

    public LocalDateTime getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(LocalDateTime uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Subcategory getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(Subcategory subcategory) {
        this.subcategory = subcategory;
    }

    public boolean isEmbedding() {
        return embedding;
    }

    public void setEmbedding(boolean embedding) {
        this.embedding = embedding;
    }

    @Override
    public String toString() {
        return "File{" +
               "id=" + id +
               ", fileName='" + fileName + '\'' +
               ", fileType='" + fileType + '\'' +
               ", size=" + size +
               ", storageIdentifier='" + storageIdentifier + '\'' +
               ", uploadTimestamp=" + uploadTimestamp +
               ", categoryId=" + (category != null ? category.getId() : "null") +
               ", subcategoryId=" + (subcategory != null ? subcategory.getId() : "null") +
               ", embedding=" + embedding +
               '}';
    }

    // Consider adding equals() and hashCode() based on 'id' or 'storageIdentifier'
}
