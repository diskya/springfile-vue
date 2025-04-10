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
    private List<File> files = new ArrayList<>();

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

    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }

    // Helper methods for bidirectional relationship
    public void addFile(File file) {
        files.add(file);
        file.setSubcategory(this);
    }

    public void removeFile(File file) {
        files.remove(file);
        file.setSubcategory(null);
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
