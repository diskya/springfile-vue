package com.example.springfile.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "subcategory", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StoredFile> storedFiles = new ArrayList<>();

    // Constructors
    public Subcategory() {
    }

    public Subcategory(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<StoredFile> getStoredFiles() {
        return storedFiles;
    }

    public void setStoredFiles(List<StoredFile> storedFiles) {
        this.storedFiles = storedFiles;
    }

    // Helper methods for bidirectional relationship
    public void addStoredFile(StoredFile storedFile) {
        storedFiles.add(storedFile);
        storedFile.setSubcategory(this);
    }

    public void removeStoredFile(StoredFile storedFile) {
        storedFiles.remove(storedFile);
        storedFile.setSubcategory(null);
    }

    @Override
    public String toString() {
        return "Subcategory{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", categoryId=" + (category != null ? category.getId() : "null") +
               '}';
    }

    // Consider adding equals() and hashCode() based on 'id' or 'name' + 'category'
}
