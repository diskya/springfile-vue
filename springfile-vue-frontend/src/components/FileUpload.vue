<template>
  <div class="file-upload-container">
    <h2>File Upload</h2>

    <form @submit.prevent="handleSubmit">
      <!-- File Input (hidden, allows multiple) -->
      <input
          type="file"
          ref="fileInput"
          @change="handleFileChange"
          :disabled="isUploading"
          style="display: none"
          multiple
        />

      <!-- Integrated File Grid -->
      <div class="file-grid" @dragover.prevent @drop.prevent="handleFileDrop">
        <div class="upload-placeholder" @click="openFileSelector">
          <div class="upload-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
          </div>
          <p v-if="selectedFiles.length === 0">Click or drag files to upload</p>
          <div v-else class="selected-files-list">
            <!-- <p>Selected files:</p> -->
            <ul>
              <li v-for="file in selectedFiles" :key="file.name">
                {{ file.name }} ({{ formatFileSize(file.size) }})
              </li>
            </ul>
          </div>
        </div>
      </div>
      <!-- End Integrated File Grid -->

      <!-- Category Selection -->
      <div class="form-group">
        <label for="category">Category</label>
        <select
          id="category"
          v-model="selectedCategoryId"
          @change="handleCategoryChange"
          :disabled="isUploading || categoriesData.length === 0"
          required
        >
          <option value="">-- Select a category --</option>
          <option
            v-for="category in categoriesData"
            :key="category.id"
            :value="category.id"
          >
            {{ category.name }}
          </option>
        </select>
      </div>

      <!-- Subcategory Selection -->
      <div class="form-group">
        <label for="subcategory">Subcategory</label>
        <select
          id="subcategory"
          v-model="selectedSubcategoryId"
          :disabled="!selectedCategoryId || isUploading"
          >
          <option value="">-- Select a subcategory (Optional) --</option>
          <option
            v-for="subcategory in availableSubcategories"
            :key="subcategory.id"
            :value="subcategory.id"
          >
            {{ subcategory.name }}
          </option>
        </select>
      </div>

      <!-- Submit Button -->
      <button
        type="submit"
        :disabled="!isFormValid || isUploading"
        class="submit-button"
      >
        {{ isUploading ? 'Uploading...' : `Upload ${selectedFiles.length} File(s)` }}
      </button>

      <!-- Status Message -->
      <div v-if="message" :class="['message', message.includes('Error') ? 'error' : 'success']">
        {{ message }}
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

// Emits
const emit = defineEmits(['upload-success']);

// Local state for categories
const categoriesData = ref([]);
const categoriesError = ref('');

// File handling refs
const fileInput = ref(null);
const selectedFiles = ref([]); // Changed to array for multiple files
// const previewUrl = ref(null); // Removed single preview
const isUploading = ref(false);
const message = ref('');
const uploadResults = ref([]); // To store results of individual file uploads
const uploadErrors = ref([]); // To store errors from individual file uploads

// Category selection refs
const selectedCategoryId = ref('');
const selectedSubcategoryId = ref('');

// Computed properties
const selectedCategory = computed(() => {
  return categoriesData.value.find(category => category.id === selectedCategoryId.value) || null;
});

const availableSubcategories = computed(() => {
  if (!selectedCategory.value) return [];
  return selectedCategory.value.subcategories || [];
});

const isFormValid = computed(() => {
  // Check if at least one file is selected and a category is chosen
  return selectedFiles.value.length > 0 && selectedCategoryId.value;
});

// Methods
const openFileSelector = () => {
  fileInput.value.click();
};

const handleFileChange = (event) => {
  const files = event.target.files;
  if (files && files.length > 0) {
    message.value = '';
    uploadResults.value = [];
    uploadErrors.value = [];
    selectedFiles.value = Array.from(files); // Convert FileList to array
    // previewUrl.value = null; // Clear preview if any
  }
};

const handleFileDrop = (event) => {
  const files = event.dataTransfer.files;
  if (files && files.length > 0) {
    message.value = '';
    uploadResults.value = [];
    uploadErrors.value = [];
    selectedFiles.value = Array.from(files); // Convert FileList to array
    // previewUrl.value = null; // Clear preview if any
  }
};

// Removed createPreview function as we are not previewing multiple files

const handleCategoryChange = () => {
  selectedSubcategoryId.value = '';
};

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
};

const handleSubmit = async () => {
  if (!isFormValid.value) return;

  isUploading.value = true;
  message.value = 'Uploading...';
  uploadResults.value = []; // Reset results
  uploadErrors.value = []; // Reset errors

  try {
    // Prepare form data
    const formData = new FormData();
    // Append each file with the key 'files'
    selectedFiles.value.forEach(file => {
      formData.append('files', file); // Use 'files' as key
    });
    formData.append('category_id', selectedCategoryId.value);
    // Only append subcategory_id if it's selected
    if (selectedSubcategoryId.value) {
      formData.append('subcategory_id', selectedSubcategoryId.value);
    }

    // Actual API call
    const response = await fetch('/api/files/upload', {
      method: 'POST',
      body: formData
    });

    const result = await response.json(); // Always try to parse JSON

    // Check for non-OK status, excluding 207 Multi-Status which indicates partial success
    if (!response.ok && response.status !== 207) {
      throw new Error(result.message || `Upload failed with status: ${response.status}`);
    }

    // Handle potential partial success (207 Multi-Status) or full success (201 Created)
    message.value = result.message || 'Upload process completed.';
    uploadResults.value = result.uploadedFiles || [];
    uploadErrors.value = result.errors || [];

    // Emit success only if there were *no* errors reported by the backend
    if (uploadErrors.value.length === 0 && response.ok) { // Ensure response was fully OK (e.g., 201)
        resetForm(); // Reset only on full success
        emit('upload-success');
    } else {
        // Clear selected files even on partial failure or errors,
        // so user has to re-select if they want to retry.
        selectedFiles.value = [];
        if (fileInput.value) fileInput.value.value = ''; // Clear input value
    }

  } catch (error) {
    console.error("Upload error details:", error);
    message.value = `Error: ${error.message}`;
    // Add general network/parsing errors to the list
    uploadErrors.value.push(`Client-side error: ${error.message}`);
    // Clear selected files on general error too
    selectedFiles.value = [];
    if (fileInput.value) fileInput.value.value = '';
  } finally {
    isUploading.value = false;
    // Keep message displayed to show results/errors
  }
};

const resetForm = () => {
  selectedFiles.value = []; // Clear the array
  // previewUrl.value = null; // Preview removed
  selectedCategoryId.value = '';
  selectedSubcategoryId.value = '';
  uploadResults.value = []; // Clear results
  uploadErrors.value = []; // Clear errors
  if (fileInput.value) {
    fileInput.value.value = ''; // Important to allow re-selecting the same file(s)
  }
  // Clear message after a longer delay only if resetForm is called (i.e., on full success)
  setTimeout(() => {
    message.value = '';
  }, 5000); // Increased delay
};

const loadCategoriesData = async () => {
  categoriesError.value = '';
  try {
    const response = await fetch('/api/categories');
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to load categories: ${response.status} ${response.statusText} - ${errorText}`);
    }
    const data = await response.json();
    categoriesData.value = data;
  } catch (error) {
    console.error('Error loading categories:', error);
    categoriesError.value = `Error loading categories: ${error.message}`;
    categoriesData.value = [];
  }
};

// Load categories when the component mounts
onMounted(() => {
  loadCategoriesData();
});

</script>

<style scoped>
.file-upload-container {
  max-width: 500px;
  margin: 20px auto 40px auto; /* Added bottom margin */
  padding: 25px;
  background: var(--color-background-soft); /* Use CSS variable */
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); /* Softer shadow */
  border: 1px solid var(--color-border); /* Subtle border */
}

h2 {
  text-align: center;
  margin-bottom: 25px;
  color: var(--color-heading); /* Use CSS variable */
  font-weight: 600;
}

.file-grid {
  margin-bottom: 20px;
}

.upload-placeholder {
  border: 2px dashed var(--color-border); /* Use CSS variable */
  border-radius: 6px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.3s ease, background-color 0.3s ease;
  background-color: var(--color-background); /* Slightly different background */
}

.upload-placeholder:hover {
  border-color: var(--color-border-hover); /* Use CSS variable */
  background-color: var(--color-background-mute);
}

.upload-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
  color: var(--color-text); /* Use CSS variable */
  opacity: 0.7;
}

.upload-placeholder p {
  color: var(--color-text);
  font-size: 0.95em;
}

.file-preview {
  margin-top: 15px;
  text-align: center;
}

.file-preview img {
  max-height: 200px;
  max-width: 100%;
  border-radius: 4px;
}

.selected-files-list {
  margin-top: 10px;
  text-align: left;
  font-size: 0.9em;
  max-height: 150px; /* Limit height */
  overflow-y: auto; /* Add scroll for many files */
  padding: 5px 10px;
  background-color: var(--color-background-mute);
  border-radius: 4px;
}

.selected-files-list ul {
  list-style: none;
  padding: 0;
  margin: 5px 0 0 0;
}

.selected-files-list li {
  padding: 2px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}


.form-group {
  margin-bottom: 15px;
}

label {
  display: block;
  margin-bottom: 6px; /* Slightly increased */
  font-weight: 500;
  color: var(--color-text); /* Use CSS variable */
  font-size: 0.9em;
}

select {
  width: 100%;
  padding: 10px 12px; /* Increased padding */
  border: 1px solid var(--color-border); /* Use CSS variable */
  border-radius: 4px;
  background-color: var(--color-background); /* Use CSS variable */
  font-size: 1em; /* Relative font size */
  color: var(--color-text);
  transition: border-color 0.3s ease;
}

select:focus {
  outline: none;
  border-color: var(--color-border-hover); /* Use CSS variable */
}

select:disabled {
  background-color: var(--color-background-mute); /* Use CSS variable */
  cursor: not-allowed;
  opacity: 0.7;
}

.submit-button {
  width: 100%;
  padding: 12px; /* Increased padding */
  /* A common theme color, adjust if needed */
  background-color: hsla(160, 100%, 37%, 1);
  color: var(--vt-c-white); /* Use CSS variable */
  border: none;
  border-radius: 4px;
  font-size: 1em; /* Relative font size */
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease, opacity 0.3s ease;
}

.submit-button:hover:not(:disabled) {
  background-color: hsla(160, 100%, 30%, 1); /* Darker shade */
}

.submit-button:disabled {
  background-color: var(--color-background-mute); /* Use CSS variable */
  color: var(--vt-c-text-light-2);
  cursor: not-allowed;
  opacity: 0.6;
}

.message {
  margin-top: 15px;
  padding: 10px; /* Increased padding */
  border-radius: 4px;
  text-align: center;
  font-size: 0.9em;
}

/* Define specific colors using variables or appropriate shades */
.success {
  background-color: hsla(160, 100%, 37%, 0.1);
  color: hsla(160, 100%, 30%, 1);
  border: 1px solid hsla(160, 100%, 37%, 0.3);
}

.error {
  background-color: hsla(0, 80%, 60%, 0.1);
  color: hsla(0, 70%, 45%, 1);
  border: 1px solid hsla(0, 80%, 60%, 0.3);
}
</style>
