<template>
  <div class="space-y-4">
    <h2 class="text-lg font-semibold text-gray-700">Labels</h2>
    
    <div>
      <label class="block text-sm font-medium text-gray-700 mb-1">Add Labels</label>
      <div class="flex">
        <input
          type="text"
          v-model="newLabel"
          @keydown.enter.prevent="addLabel"
          placeholder="Type a label and press Enter"
          class="block w-full px-3 py-2 border border-gray-300 rounded-l-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
        />
        <button
          @click="addLabel"
          class="px-4 py-2 bg-blue-600 text-white font-medium rounded-r-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2">
          Add
        </button>
      </div>
    </div>
    
    <div v-if="suggestedLabels.length">
      <label class="block text-sm font-medium text-gray-700 mb-2">Suggested Labels</label>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="label in suggestedLabels"
          :key="label"
          @click="toggleSuggestedLabel(label)"
          class="px-2 py-1 text-sm rounded-full"
          :class="localLabels.includes(label) ? 'bg-blue-100 text-blue-800 border border-blue-300' : 'bg-gray-100 text-gray-800 border border-gray-300'"
        >
          {{ label }}
        </button>
      </div>
    </div>
    
    <div v-if="localLabels.length">
      <label class="block text-sm font-medium text-gray-700 mb-2">Selected Labels</label>
      <div class="flex flex-wrap gap-2">
        <div
          v-for="(label, index) in localLabels"
          :key="index"
          class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full flex items-center"
        >
          <span>{{ label }}</span>
          <button @click="removeLabel(index)" class="ml-2 text-blue-600 hover:text-blue-800">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  labels: {
    type: Array,
    default: () => []
  },
  suggestedLabels: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['update:labels']);

const localLabels = ref([...props.labels]);
const newLabel = ref('');

watch(() => props.labels, (newValue) => {
  localLabels.value = [...newValue];
});

const addLabel = () => {
  if (newLabel.value.trim() && !localLabels.value.includes(newLabel.value.trim())) {
    localLabels.value.push(newLabel.value.trim());
    emit('update:labels', localLabels.value);
    newLabel.value = '';
  }
};

const removeLabel = (index) => {
  localLabels.value.splice(index, 1);
  emit('update:labels', localLabels.value);
};

const toggleSuggestedLabel = (label) => {
  const index = localLabels.value.indexOf(label);
  if (index === -1) {
    localLabels.value.push(label);
  } else {
    localLabels.value.splice(index, 1);
  }
  emit('update:labels', localLabels.value);
};
</script>