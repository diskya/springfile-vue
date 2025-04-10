<template>
  <div class="file-list-container">
    <h2>Uploaded Files</h2>

    <!-- Search Input -->
    <div class="search-container">
      <input type="text" v-model="searchTerm" placeholder="Search by name, category, or subcategory..." />
    </div>

    <!-- Loading/Error State -->
    <div v-if="isLoadingFiles" class="loading-message">Loading files...</div>
    <div v-if="filesError" class="error-message">{{ filesError }}</div>

    <!-- Files Table Wrapper for Responsiveness -->
    <div class="table-wrapper" v-if="!isLoadingFiles && !filesError && filteredFiles.length > 0">
      <table class="files-table">
        <thead>
          <tr>
            <th>File Name</th>
          <th>Type</th>
          <th>Size</th>
          <th>Category</th>
          <th>Subcategory</th>
          <th>Uploaded At</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="file in filteredFiles" :key="file.id">
          <td>{{ file.fileName }}</td>
          <td>{{ file.fileType }}</td>
          <td>{{ formatFileSize(file.size) }}</td>
          <td>{{ file.categoryName || 'N/A' }}</td>
          <td>{{ file.subcategoryName || 'N/A' }}</td>
          <td>{{ formatTimestamp(file.uploadTimestamp) }}</td>
        </tr>
      </tbody>
      </table>
    </div>

    <!-- No Files Message -->
    <div v-if="!isLoadingFiles && !filesError && filteredFiles.length === 0" class="no-files-message">
      {{ searchTerm ? 'No files match your search.' : 'No files uploaded yet.' }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

const filesList = ref([]);
const isLoadingFiles = ref(false);
const filesError = ref('');
const searchTerm = ref('');

const filteredFiles = computed(() => {
  if (!searchTerm.value) {
    return filesList.value;
  }
  const lowerSearchTerm = searchTerm.value.toLowerCase();
  return filesList.value.filter(file =>
    (file.fileName && file.fileName.toLowerCase().includes(lowerSearchTerm)) ||
    (file.categoryName && file.categoryName.toLowerCase().includes(lowerSearchTerm)) ||
    (file.subcategoryName && file.subcategoryName.toLowerCase().includes(lowerSearchTerm))
  );
});

const formatFileSize = (bytes) => {
  if (bytes === undefined || bytes === null) return 'N/A';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
};

const formatTimestamp = (timestamp) => {
  if (!timestamp) return 'N/A';
  try {
    const date = new Date(timestamp);
    return isNaN(date.getTime()) ? timestamp : date.toLocaleString();
  } catch (e) {
    console.error("Error formatting timestamp:", e);
    return timestamp;
  }
};

const fetchFiles = async () => {
  isLoadingFiles.value = true;
  filesError.value = '';
  try {
    const response = await fetch('/api/files');
    if (!response.ok) {
      if (response.status === 204) {
        filesList.value = [];
        return;
      }
      const errorText = await response.text();
      throw new Error(`Failed to load files: ${response.status} ${response.statusText} - ${errorText}`);
    }
    const data = await response.json();
    filesList.value = data.sort((a, b) => new Date(b.uploadTimestamp) - new Date(a.uploadTimestamp));
  } catch (error) {
    console.error('Error fetching files:', error);
    filesError.value = `Error loading files: ${error.message}`;
    filesList.value = [];
  } finally {
    isLoadingFiles.value = false;
  }
};

defineExpose({
  fetchFiles
});

onMounted(() => {
  fetchFiles();
});
</script>

<style scoped>
.file-list-container {
  margin-top: 0; /* Removed top margin, handled by App.vue spacing */
  width: 100%;
  background: var(--color-background-soft); /* Use CSS variable */
  padding: 25px; /* Consistent padding */
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08); /* Softer shadow */
  border: 1px solid var(--color-border); /* Subtle border */
}

.file-list-container h2 {
  text-align: center;
  margin-bottom: 25px; /* Consistent margin */
  color: var(--color-heading); /* Use CSS variable */
  font-weight: 600;
}

.search-container {
  margin-bottom: 20px;
}

.search-container input {
  width: 100%;
  padding: 10px 15px; /* Keep padding */
  border: 1px solid var(--color-border); /* Use CSS variable */
  border-radius: 4px;
  font-size: 1em; /* Relative font size */
  box-sizing: border-box;
  background-color: var(--color-background); /* Use CSS variable */
  color: var(--color-text);
  transition: border-color 0.3s ease;
}

.search-container input:focus {
  outline: none;
  border-color: var(--color-border-hover); /* Use CSS variable */
}

.files-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
  table-layout: fixed; /* Prevent width changes based on content */
  min-width: 700px; /* Ensure a minimum width, forcing scroll when needed */
}

.table-wrapper {
  overflow-x: auto; /* Enable horizontal scrolling on smaller screens */
  width: 100%;
}

.files-table th,
.files-table td {
  border: 1px solid var(--color-border); /* Use CSS variable */
  padding: 10px 12px;
  text-align: left;
  font-size: 0.9em; /* Relative font size */
  vertical-align: middle;
  /* Allow long words to break and wrap */
  overflow-wrap: break-word;
  word-break: break-word; /* Use break-word for better compatibility */
}

.files-table th {
  background-color: var(--color-background-mute); /* Use CSS variable */
  font-weight: 600;
  white-space: nowrap;
  color: var(--color-heading);
}

.files-table tbody tr:nth-child(even) {
  background-color: var(--color-background); /* Use CSS variable */
}

.files-table tbody tr:hover {
  background-color: var(--color-background-mute); /* Use CSS variable */
}

.loading-message,
.error-message,
.no-files-message {
  text-align: center;
  padding: 20px;
  margin-top: 15px;
  color: var(--vt-c-text-light-2); /* Use CSS variable */
  font-size: 0.95em;
}

.error-message {
  /* Use consistent error styling from FileUpload */
  background-color: hsla(0, 80%, 60%, 0.1);
  color: hsla(0, 70%, 45%, 1);
  border: 1px solid hsla(0, 80%, 60%, 0.3);
  border-radius: 4px;
  padding: 10px; /* Consistent padding */
}
</style>
