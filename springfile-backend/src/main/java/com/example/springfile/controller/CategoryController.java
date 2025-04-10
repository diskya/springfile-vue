package com.example.springfile.controller;

import com.example.springfile.dto.CategoryDto;
import com.example.springfile.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
// Removed ResponseEntity import
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// Removed ResponseStatus import if it was implicitly there

import java.util.List;

@RestController
@RequestMapping("/api/categories") // Base path for all endpoints in this controller
public class CategoryController {

    private final CategoryService categoryService;

    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Handles GET requests to /api/categories.
     * Retrieves all categories with their nested subcategories via the CategoryService
     * and returns them directly as a JSON response body.
     * Spring MVC will automatically set the HTTP status to 200 OK.
     *
     * @return A list of CategoryDto objects.
     */
    @GetMapping
    // Changed return type from ResponseEntity<List<CategoryDto>> to List<CategoryDto>
    public List<CategoryDto> getAllCategoriesWithSubcategories() {
        List<CategoryDto> categories = categoryService.getAllCategoriesWithSubcategories();
        return categories; // Return the list directly
    }
}
