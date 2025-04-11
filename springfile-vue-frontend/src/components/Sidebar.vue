<template>
  <aside class="sidebar">
    <div class="navigation-section">
      <h4>Categories</h4>
      <ul v-if="categories.length" class="category-list">
        <li v-for="category in categories" :key="category.id" class="category-item">
          <span>{{ category.name }}</span>
          <ul v-if="category.subcategories && category.subcategories.length" class="subcategory-list">
            <li v-for="subcategory in category.subcategories" :key="subcategory.id" class="subcategory-item">
              {{ subcategory.name }}
            </li>
          </ul>
        </li>
      </ul>
      <p v-else-if="categoriesError">{{ categoriesError }}</p>
      <p v-else>Loading categories...</p>
    </div>
    <div class="upload-section">
      <FileUpload @upload-success="onUploadSuccess" />
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import FileUpload from './FileUpload.vue';

const categories = ref([]);
const categoriesError = ref('');

// Define emits to bubble up the upload-success event
const emit = defineEmits(['upload-success']);

const onUploadSuccess = () => {
  emit('upload-success');
};

const fetchCategories = async () => {
  categoriesError.value = '';
  try {
    const response = await fetch('/api/categories'); // Using fetch API
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to load categories: ${response.status} ${response.statusText} - ${errorText}`);
    }
    const data = await response.json();
    categories.value = data;
  } catch (error) {
    console.error('Error fetching categories:', error);
    categoriesError.value = `Error: Could not load categories.`; // User-friendly error
    categories.value = []; // Ensure categories is empty on error
  }
};

// Fetch categories when the component mounts
onMounted(fetchCategories);

</script>

<style scoped>
.sidebar {
  width: 350px; /* Increased width */
  height: 100vh; /* Full height */
  position: fixed; /* Fixed position */
  left: 0;
  top: 0;
  background-color: var(--color-background-soft); /* Use theme variable */
  border-right: 1px solid var(--color-border);
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between; /* Pushes upload to the bottom */
}

.navigation-section {
  /* Styles for the navigation area */
  flex-grow: 1; /* Allows this section to take up available space */
  overflow-y: auto; /* Add scroll if content overflows */
  padding-right: 10px; /* Add some padding for scrollbar */
}

.navigation-section h4 {
  margin-bottom: 15px;
  color: var(--color-heading);
}

.category-list, .subcategory-list {
  list-style: none;
  padding-left: 0;
}

.category-item {
  margin-bottom: 10px;
}

.category-item > span {
  font-weight: 500;
  cursor: pointer; /* Optional: Add interaction later */
  display: block; /* Make span take full width */
  padding: 5px 0;
}

.subcategory-list {
  padding-left: 20px; /* Indent subcategories */
  margin-top: 5px;
}

.subcategory-item {
  padding: 3px 0;
  font-size: 0.9em;
  color: var(--color-text-mute); /* Slightly muted color */
  cursor: pointer; /* Optional: Add interaction later */
}

.upload-section {
  /* Styles for the upload area */
  margin-top: 20px; /* Space above the upload component */
  padding-top: 20px; /* Add padding above upload */
  border-top: 1px solid var(--color-border); /* Separator line */
}
</style>
