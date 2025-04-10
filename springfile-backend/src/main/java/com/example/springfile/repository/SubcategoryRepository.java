package com.example.springfile.repository;

import com.example.springfile.model.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {
    // JpaRepository provides findById(Long id)
}
