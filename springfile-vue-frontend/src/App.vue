<template>
  <div class="file-upload-container">
    <h2>File Upload</h2>
    
    <form @submit.prevent="handleSubmit">
      <!-- File Upload Area -->
      <div class="upload-area" @dragover.prevent @drop.prevent="handleFileDrop">
        <input 
          type="file" 
          ref="fileInput" 
          @change="handleFileChange" 
          :disabled="isUploading"
          style="display: none"
        />
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
      
      <!-- Category Selection -->
      <div class="form-group">
        <label for="category">Category</label>
        <select 
          id="category" 
          v-model="selectedCategoryId" 
          @change="handleCategoryChange" 
          :disabled="isUploading"
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

// File handling refs
const fileInput = ref(null);
const selectedFile = ref(null);
const previewUrl = ref(null);
const isUploading = ref(false);
const message = ref('');

// Category selection refs
const selectedCategoryId = ref('');
const selectedSubcategoryId = ref('');

// Categories data fetched from API
const categoriesData = ref([]);

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
    message.value = ''; // Clear previous message
    selectedFile.value = file;
    createPreview(file);
  }
};

const handleFileDrop = (event) => {
  const file = event.dataTransfer.files[0];
  if (file) {
    message.value = ''; // Clear previous message
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
  // Reset subcategory when category changes
  selectedSubcategoryId.value = '';
};

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
};

const handleSubmit = async () => {
  if (!isFormValid.value) return;
  
  isUploading.value = true;
  message.value = 'Uploading...';
  
  try {
    // Prepare form data - only sending IDs
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    formData.append('category_id', selectedCategoryId.value);
    formData.append('subcategory_id', selectedSubcategoryId.value);

    // Actual API call
    const response = await fetch('/api/files/upload', { // Updated endpoint
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
  // Reset file input
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

// Function to load categories data from an API
const loadCategoriesData = async () => {
  try {
    // Fetch categories from the backend API
    const response = await fetch('/api/categories'); // Assuming the backend runs on the same host/port or is proxied
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Failed to load categories: ${response.status} ${response.statusText} - ${errorText}`);
    }
    const data = await response.json();
    categoriesData.value = data;
    
  } catch (error) {
    console.error('Error loading categories:', error);
    message.value = `Error loading categories: ${error.message}`;
    // Optionally clear categories if loading fails
    // categoriesData.value = []; 
  }
};

// Load data when component mounts
onMounted(() => {
  loadCategoriesData();
});
</script>

<style scoped>
.file-upload-container {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.upload-area {
  margin-bottom: 20px;
}

.upload-placeholder {
  border: 2px dashed #ddd;
  border-radius: 6px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-placeholder:hover {
  border-color: #4299e1;
}

.upload-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
  color: #718096;
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
  margin-bottom: 5px;
  font-weight: 500;
  color: #4a5568;
}

select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  font-size: 16px;
}

select:disabled {
  background-color: #f7fafc;
  cursor: not-allowed;
}

.submit-button {
  width: 100%;
  padding: 10px;
  background-color: #4299e1;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.submit-button:hover:not(:disabled) {
  background-color: #3182ce;
}

.submit-button:disabled {
  background-color: #a0aec0;
  cursor: not-allowed;
}

.message {
  margin-top: 15px;
  padding: 8px;
  border-radius: 4px;
  text-align: center;
}

.success {
  background-color: #c6f6d5;
  color: #2f855a;
}

.error {
  background-color: #fed7d7;
  color: #c53030;
}
</style>
