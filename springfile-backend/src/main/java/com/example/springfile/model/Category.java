package com.example.springfile.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Subcategory> subcategories = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StoredFile> storedFiles = new ArrayList<>();

    // Constructors
    public Category() {
    }

    public Category(String name) {
        this.name = name;
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

    public List<Subcategory> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<Subcategory> subcategories) {
        this.subcategories = subcategories;
    }

    public List<StoredFile> getStoredFiles() {
        return storedFiles;
    }

    public void setStoredFiles(List<StoredFile> storedFiles) {
        this.storedFiles = storedFiles;
    }

    // Helper methods for bidirectional relationship (optional but recommended)
    public void addSubcategory(Subcategory subcategory) {
        subcategories.add(subcategory);
        subcategory.setCategory(this);
    }

    public void removeSubcategory(Subcategory subcategory) {
        subcategories.remove(subcategory);
        subcategory.setCategory(null);
    }

    public void addStoredFile(StoredFile storedFile) {
        storedFiles.add(storedFile);
        storedFile.setCategory(this);
    }

    public void removeStoredFile(StoredFile storedFile) {
        storedFiles.remove(storedFile);
        storedFile.setCategory(null);
    }

    @Override
    public String toString() {
        return "Category{" +
               "id=" + id +
               ", name='" + name + '\'' +
               '}';
    }

    // Consider adding equals() and hashCode() based on 'id' or 'name'
}
