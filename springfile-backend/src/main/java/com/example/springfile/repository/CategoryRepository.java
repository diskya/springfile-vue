package com.example.springfile.repository;

import com.example.springfile.model.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds all categories and eagerly fetches their associated subcategories
     * using an EntityGraph to prevent N+1 query problems.
     *
     * @return A list of Category entities with their subcategories initialized.
     */
    @Override
    @EntityGraph(attributePaths = {"subcategories"})
    List<Category> findAll();
}
