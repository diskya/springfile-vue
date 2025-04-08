<template>
  <div class="border rounded-lg p-4 bg-gray-50">
    <h2 class="text-lg font-semibold text-gray-700 mb-4">File Preview</h2>
    
    <div class="grid md:grid-cols-2 gap-4">
      <div>
        <div class="border rounded p-4 bg-white h-full">
          <div class="flex items-center mb-4">
            <svg class="w-8 h-8 text-gray-500 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
            </svg>
            <div>
              <h3 class="font-medium truncate" :title="file.name">{{ file.name }}</h3>
              <p class="text-sm text-gray-500">{{ fileSize }}</p>
            </div>
          </div>
          
          <div v-if="isImage" class="flex justify-center">
            <img :src="filePreviewUrl" alt="File preview" class="max-h-64 object-contain" />
          </div>
          
          <div v-else-if="isPDF" class="text-center py-6">
            <p class="text-gray-600">PDF file preview not available</p>
          </div>
          
          <div v-else class="text-center py-6">
            <p class="text-gray-600">Preview not available for this file type</p>
          </div>
        </div>
      </div>
      
      <div>
        <div class="border rounded p-4 bg-white h-full">
          <h3 class="font-medium mb-2">File Details</h3>
          
          <dl class="space-y-2">
            <div class="grid grid-cols-3 gap-4">
              <dt class="text-sm font-medium text-gray-500">Type:</dt>
              <dd class="text-sm text-gray-900 col-span-2">{{ file.type || "Unknown" }}</dd>
            </div>
            
            <div class="grid grid-cols-3 gap-4">
              <dt class="text-sm font-medium text-gray-500">Category:</dt>
              <dd class="text-sm text-gray-900 col-span-2">{{ category || "Not selected" }}</dd>
            </div>
            
            <div class="grid grid-cols-3 gap-4">
              <dt class="text-sm font-medium text-gray-500">Subcategory:</dt>
              <dd class="text-sm text-gray-900 col-span-2">{{ subcategory || "Not selected" }}</dd>
            </div>
            
            <div v-if="labels.length">
              <dt class="text-sm font-medium text-gray-500 mb-1">Labels:</dt>
              <dd class="text-sm text-gray-900">
                <div class="flex flex-wrap gap-1 mt-1">
                  <span 
                    v-for="(label, index) in labels" 
                    :key="index"
                    class="px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded-full"
                  >
                    {{ label }}
                  </span>
                </div>
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';

const props = defineProps({
  file: {
    type: Object,
    required: true
  },
  category: {
    type: String,
    default: ''
  },
  subcategory: {
    type: String,
    default: ''
  },
  labels: {
    type: Array,
    default: () => []
  }
});

const filePreviewUrl = ref('');

const isImage = computed(() => {
  return props.file.type.startsWith('image/');
});

const isPDF = computed(() => {
  return props.file.type === 'application/pdf';
});

const fileSize = computed(() => {
  const size = props.file.size;
  if (size < 1024) {
    return size + ' bytes';
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB';
  } else {
    return (size / (1024 * 1024)).toFixed(2) + ' MB';
  }
});

const createPreviewUrl = () => {
  if (isImage.value) {
    filePreviewUrl.value = URL.createObjectURL(props.file);
  }
};

onMounted(() => {
  createPreviewUrl();
});

watch(() => props.file, () => {
  if (filePreviewUrl.value) {
    URL.revokeObjectURL(filePreviewUrl.value);
  }
  createPreviewUrl();
});

// Clean up when component is unmounted
onUnmounted(() => {
  if (filePreviewUrl.value) {
    URL.revokeObjectURL(filePreviewUrl.value);
  }
});
</script>