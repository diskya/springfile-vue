package com.example.springfile.service;

import com.example.springfile.dto.CategoryDto;
import com.example.springfile.dto.SubcategoryDto;
import com.example.springfile.model.Category;
import com.example.springfile.model.Subcategory;
import com.example.springfile.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Retrieves all categories along with their nested subcategories, mapped to DTOs.
     * Uses the repository method with EntityGraph to ensure efficient fetching.
     *
     * @return A list of CategoryDto objects, each containing its subcategories.
     */
    @Transactional(readOnly = true) // Ensures data consistency and potentially optimizes read operations
    public List<CategoryDto> getAllCategoriesWithSubcategories() {
        List<Category> categories = categoryRepository.findAll(); // Use the standard findAll with EntityGraph
        return categories.stream()
                         .map(this::convertToCategoryDto)
                         .collect(Collectors.toList());
    }

    /**
     * Converts a Category entity to its corresponding CategoryDto, including nested subcategories.
     *
     * @param category The Category entity to convert.
     * @return The corresponding CategoryDto.
     */
    private CategoryDto convertToCategoryDto(Category category) {
        List<SubcategoryDto> subcategoryDtos = category.getSubcategories().stream()
                                                       .map(this::convertToSubcategoryDto)
                                                       .collect(Collectors.toList());
        return new CategoryDto(
            category.getId(),
            category.getName(),
            subcategoryDtos
        );
    }

    /**
     * Converts a Subcategory entity to its corresponding SubcategoryDto.
     *
     * @param subcategory The Subcategory entity to convert.
     * @return The corresponding SubcategoryDto.
     */
    private SubcategoryDto convertToSubcategoryDto(Subcategory subcategory) {
        return new SubcategoryDto(
            subcategory.getId(),
            subcategory.getName()
        );
    }
}
