<template>
  <div class="search-bar">
    <input
      type="text"
      v-model="searchQuery"
      placeholder="Search documents..."
      @keyup.enter="performSearch"
      class="search-input"
    />
    <button @click="performSearch" class="search-button">Search</button>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const searchQuery = ref('');
const emit = defineEmits(['search']);

const performSearch = () => {
  if (searchQuery.value.trim()) {
    emit('search', searchQuery.value.trim());
  }
  // Optional: Clear the input after search?
  // searchQuery.value = '';
};
</script>

<style scoped>
.search-bar {
  display: flex;
  align-items: center;
  width: 100%;
  max-width: 600px; /* Or adjust as needed */
  margin: 0 auto; /* Center the search bar */
}

.search-input {
  flex-grow: 1;
  padding: 10px 15px;
  border: 1px solid #ccc;
  border-radius: 4px 0 0 4px; /* Rounded left corners */
  font-size: 1rem;
  outline: none;
}

.search-input:focus {
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.search-button {
  padding: 10px 20px;
  border: 1px solid #007bff;
  background-color: #007bff;
  color: white;
  border-radius: 0 4px 4px 0; /* Rounded right corners */
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s ease;
}

.search-button:hover {
  background-color: #0056b3;
  border-color: #0056b3;
}
</style>
