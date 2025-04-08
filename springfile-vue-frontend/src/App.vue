<template>
  <div class="min-h-screen bg-gray-50">
    <header class="bg-white shadow">
      <div class="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <h1 class="text-3xl font-bold text-gray-900">File Upload Manager</h1>
      </div>
    </header>
    
    <main class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
      <div class="bg-white shadow rounded-lg p-6">
        <FileUploader @file-selected="onFileSelected" />
        
        <div class="mt-8 grid gap-6 md:grid-cols-2">
          <CategorySelector 
            v-model:category="selectedCategory"
            v-model:subcategory="selectedSubcategory"
            :categories="categories"
          />
          
          <LabelManager 
            v-model:labels="selectedLabels"
            :suggested-labels="suggestedLabels" 
          />
        </div>
        
        <div class="mt-8">
          <FilePreview 
            v-if="selectedFile" 
            :file="selectedFile" 
            :category="selectedCategory"
            :subcategory="selectedSubcategory"
            :labels="selectedLabels"
          />
        </div>
        
        <div class="mt-8 flex justify-end">
          <button 
            @click="submitForm"
            class="px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            :disabled="!isFormValid"
          >
            Submit
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import FileUploader from './components/FileUploader.vue';
import CategorySelector from './components/CategorySelector.vue';
import LabelManager from './components/LabelManager.vue';
import FilePreview from './components/FilePreview.vue';

// State for the selected file
const selectedFile = ref(null);

// State for categories and subcategories
const categories = ref([
  { 
    name: 'Documents', 
    subcategories: ['Reports', 'Invoices', 'Contracts', 'Other'] 
  },
  { 
    name: 'Images', 
    subcategories: ['Photos', 'Graphics', 'Screenshots', 'Other'] 
  },
  { 
    name: 'Media', 
    subcategories: ['Videos', 'Audio', 'Presentations', 'Other'] 
  },
  { 
    name: 'Custom', 
    subcategories: ['Custom'] 
  }
]);

// Selected category and subcategory
const selectedCategory = ref('');
const selectedSubcategory = ref('');

// Labels
const suggestedLabels = ref(['Important', 'Draft', 'Final', 'Archive', 'Review']);
const selectedLabels = ref([]);

// Form validation
const isFormValid = computed(() => {
  return selectedFile.value && 
         selectedCategory.value && 
         selectedSubcategory.value;
});

// Event handlers
const onFileSelected = (file) => {
  selectedFile.value = file;
};

const submitForm = () => {
  // Here you would typically send the data to your server
  // For demonstration purposes, we'll just log it
  console.log({
    file: selectedFile.value,
    category: selectedCategory.value,
    subcategory: selectedSubcategory.value,
    labels: selectedLabels.value
  });
  
  alert('File uploaded successfully!');
  
  // Reset the form
  selectedFile.value = null;
  selectedCategory.value = '';
  selectedSubcategory.value = '';
  selectedLabels.value = [];
};
</script>