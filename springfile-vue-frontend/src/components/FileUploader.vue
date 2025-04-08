<template>
  <div class="w-full">
    <label 
      for="file-upload"
      class="flex justify-center w-full h-32 px-4 transition bg-white border-2 border-gray-300 border-dashed rounded-lg appearance-none cursor-pointer hover:border-blue-500 focus:outline-none"
      :class="{ 'border-blue-500': isDragging }"
      @dragover.prevent="onDragOver"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
    >
      <span class="flex flex-col items-center justify-center space-y-2">
        <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
        </svg>
        <span class="font-medium text-gray-600">
          {{ file ? file.name : 'Drop files here or click to upload' }}
        </span>
        <span class="text-xs text-gray-500">
          (Accepts any file type)
        </span>
      </span>
    </label>
    <input 
      id="file-upload" 
      name="file-upload" 
      type="file" 
      class="hidden" 
      @change="onFileChange"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';

const emit = defineEmits(['file-selected']);
const file = ref(null);
const isDragging = ref(false);

const onFileChange = (e) => {
  const files = e.target.files || e.dataTransfer.files;
  if (!files.length) return;
  
  file.value = files[0];
  emit('file-selected', file.value);
};

const onDragOver = () => {
  isDragging.value = true;
};

const onDragLeave = () => {
  isDragging.value = false;
};

const onDrop = (e) => {
  isDragging.value = false;
  const files = e.dataTransfer.files;
  if (!files.length) return;
  
  file.value = files[0];
  emit('file-selected', file.value);
};
</script>