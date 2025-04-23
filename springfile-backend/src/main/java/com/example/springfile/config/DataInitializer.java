package com.example.springfile.config;

import com.example.springfile.model.Category;
import com.example.springfile.model.Subcategory;
import com.example.springfile.repository.CategoryRepository;
import com.example.springfile.repository.SubcategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public DataInitializer(CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    @Override
    @Transactional // Add Transactional to ensure atomicity and handle lazy loading if needed
    public void run(String... args) throws Exception {
        // Check if categories already exist
        if (categoryRepository.count() == 0) {
            log.info("No existing categories found. Initializing default categories and subcategories...");

            // Create Categories
            Category tech = new Category();
            tech.setName("Technology");

            Category health = new Category();
            health.setName("Health");

            Category finance = new Category();
            finance.setName("Finance");

            // Save Categories first to get generated IDs
            List<Category> savedCategories = categoryRepository.saveAll(Arrays.asList(tech, health, finance));
            Category savedTech = savedCategories.stream().filter(c -> "Technology".equals(c.getName())).findFirst().orElse(null);
            Category savedHealth = savedCategories.stream().filter(c -> "Health".equals(c.getName())).findFirst().orElse(null);
            Category savedFinance = savedCategories.stream().filter(c -> "Finance".equals(c.getName())).findFirst().orElse(null);

            // Create Subcategories and associate them
            if (savedTech != null) {
                Subcategory software = new Subcategory();
                software.setName("Software Development");
                software.setCategory(savedTech);

                Subcategory hardware = new Subcategory();
                hardware.setName("Hardware");
                hardware.setCategory(savedTech);
                subcategoryRepository.saveAll(Arrays.asList(software, hardware));
            }

            if (savedHealth != null) {
                Subcategory fitness = new Subcategory();
                fitness.setName("Fitness");
                fitness.setCategory(savedHealth);

                Subcategory nutrition = new Subcategory();
                nutrition.setName("Nutrition");
                nutrition.setCategory(savedHealth);
                subcategoryRepository.saveAll(Arrays.asList(fitness, nutrition));
            }

             if (savedFinance != null) {
                Subcategory investing = new Subcategory();
                investing.setName("Investing");
                investing.setCategory(savedFinance);

                Subcategory budgeting = new Subcategory();
                budgeting.setName("Budgeting");
                budgeting.setCategory(savedFinance);
                subcategoryRepository.saveAll(Arrays.asList(investing, budgeting));
            }

            log.info("Default data initialization complete. Categories: {}, Subcategories: {}", categoryRepository.count(), subcategoryRepository.count());
        } else {
            log.info("Categories already exist (count: {}). Skipping default data initialization.", categoryRepository.count());
        }
    }
}
