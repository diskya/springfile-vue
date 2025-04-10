<template>
  <div class="file-upload-container">
    <h2>File Upload</h2>

    <form @submit.prevent="handleSubmit">
      <!-- File Input (hidden) -->
      <input
          type="file"
          ref="fileInput"
          @change="handleFileChange"
          :disabled="isUploading"
          style="display: none"
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
          <p v-if="!selectedFile">Click or drag file to upload</p>
          <p v-else>{{ selectedFile.name }} ({{ formatFileSize(selectedFile.size) }})</p>
        </div>

        <!-- File Preview -->
        <div v-if="previewUrl" class="file-preview">
          <img :src="previewUrl" alt="Preview" />
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
          required
        >
          <option value="">-- Select a subcategory --</option>
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
        {{ isUploading ? 'Uploading...' : 'Upload File' }}
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
const selectedFile = ref(null);
const previewUrl = ref(null);
const isUploading = ref(false);
const message = ref('');

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
  return selectedFile.value && selectedCategoryId.value && selectedSubcategoryId.value;
});

// Methods
const openFileSelector = () => {
  fileInput.value.click();
};

const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    message.value = '';
    selectedFile.value = file;
    createPreview(file);
  }
};

const handleFileDrop = (event) => {
  const file = event.dataTransfer.files[0];
  if (file) {
    message.value = '';
    selectedFile.value = file;
    createPreview(file);
  }
};

const createPreview = (file) => {
  // Create preview for images
  if (file.type.startsWith('image/')) {
    const reader = new FileReader();
    reader.onload = (e) => {
      previewUrl.value = e.target.result;
    };
    reader.readAsDataURL(file);
  } else {
    previewUrl.value = null;
  }
};

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

  try {
    // Prepare form data
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    formData.append('category_id', selectedCategoryId.value);
    formData.append('subcategory_id', selectedSubcategoryId.value);

    // Actual API call
    const response = await fetch('/api/files/upload', {
      method: 'POST',
      body: formData
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Upload failed with status: ' + response.status }));
      throw new Error(errorData.message || 'Upload failed');
    }

    const result = await response.json();
    message.value = result.message || 'File uploaded successfully!';
    resetForm();
    emit('upload-success');
  } catch (error) {
    message.value = `Error: ${error.message}`;
  } finally {
    isUploading.value = false;
  }
};

const resetForm = () => {
  selectedFile.value = null;
  previewUrl.value = null;
  selectedCategoryId.value = '';
  selectedSubcategoryId.value = '';
  if (fileInput.value) {
    fileInput.value.value = '';
  }
  setTimeout(() => {
    message.value = '';
  }, 3000);
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
