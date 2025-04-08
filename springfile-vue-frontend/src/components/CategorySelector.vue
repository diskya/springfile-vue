<template>
  <div class="space-y-4">
    <h2 class="text-lg font-semibold text-gray-700">Categories</h2>
    
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Category</label>
      <div class="relative">
        <select
          v-model="localCategory"
          @change="updateCategory"
          class="block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="" disabled>Select a category</option>
          <option v-for="category in categories" :key="category.name" :value="category.name">
            {{ category.name }}
          </option>
        </select>
      </div>
    </div>
    
    <div v-if="localCategory === 'Custom'">
      <label class="block text-sm font-medium text-gray-700 mb-1">Custom Category</label>
      <input
        type="text"
        v-model="customCategory"
        placeholder="Enter custom category"
        class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
        @input="updateCategory"
      />
    </div>
    
    <div v-if="localCategory">
      <label class="block text-sm font-medium text-gray-700 mb-1">Subcategory</label>
      <div class="relative" v-if="availableSubcategories.length && localCategory !== 'Custom'">
        <select
          v-model="localSubcategory"
          @change="updateSubcategory"
          class="block w-full px-3 py-2 bg-white border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
        >
          <option value="" disabled>Select a subcategory</option>
          <option v-for="subcategory in availableSubcategories" :key="subcategory" :value="subcategory">
            {{ subcategory }}
          </option>
        </select>
      </div>
      
      <div v-if="localSubcategory === 'Custom' || localCategory === 'Custom'">
        <label class="block text-sm font-medium text-gray-700 mt-4 mb-1">Custom Subcategory</label>
        <input
          type="text"
          v-model="customSubcategory"
          placeholder="Enter custom subcategory"
          class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
          @input="updateSubcategory"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  categories: {
    type: Array,
    required: true
  },
  category: {
    type: String,
    default: ''
  },
  subcategory: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['update:category', 'update:subcategory']);

const localCategory = ref(props.category);
const localSubcategory = ref(props.subcategory);
const customCategory = ref('');
const customSubcategory = ref('');

const availableSubcategories = computed(() => {
  if (!localCategory.value) return [];
  const categoryObj = props.categories.find(cat => cat.name === localCategory.value);
  return categoryObj ? categoryObj.subcategories : [];
});

watch(() => props.category, (newValue) => {
  localCategory.value = newValue;
});

watch(() => props.subcategory, (newValue) => {
  localSubcategory.value = newValue;
});

const updateCategory = () => {
  const finalCategory = localCategory.value === 'Custom' ? customCategory.value : localCategory.value;
  emit('update:category', finalCategory);
  if (localCategory.value !== 'Custom') {
    localSubcategory.value = '';
    emit('update:subcategory', '');
  }
};

const updateSubcategory = () => {
  const finalSubcategory = localSubcategory.value === 'Custom' || localCategory.value === 'Custom'
    ? customSubcategory.value
    : localSubcategory.value;
  emit('update:subcategory', finalSubcategory);
};
</script>