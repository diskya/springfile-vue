<template>
  <div class="file-manager-layout">
    <Sidebar @upload-success="refreshFileList" />
    <main class="file-manager-content">
       <div class="navigation-header">
         <router-link to="/" class="nav-link">Back to Search</router-link>
         <h2>File Manager</h2>
       </div>
      <FileTable ref="fileTableRef" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import Sidebar from '../components/Sidebar.vue';
import FileTable from '../components/FileTable.vue';

const fileTableRef = ref(null);

const refreshFileList = () => {
  if (fileTableRef.value) {
    fileTableRef.value.fetchFiles();
  } else {
    console.warn("FileTable component ref not available yet for refresh.");
  }
};
</script>

<style scoped>
.file-manager-layout {
  display: flex;
  min-height: 100vh; /* Ensure layout takes full viewport height */
}

.file-manager-content {
  flex-grow: 1; /* Takes remaining space */
  margin-left: 350px; /* Match sidebar width */
  padding: 20px;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* Prevent content overflow issues */
}

.navigation-header {
  display: flex;
  align-items: center;
  justify-content: space-between; /* Pushes items to ends */
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid var(--color-border);
}

.navigation-header h2 {
  margin: 0; /* Remove default margin */
  color: var(--color-heading);
  text-align: right; /* Align heading to the right */
}

.nav-link {
  padding: 6px 12px;
  background-color: var(--vt-c-indigo);
  color: white;
  text-decoration: none;
  border-radius: 4px;
  font-size: 0.9em;
  transition: background-color 0.3s ease;
}

.nav-link:hover {
  background-color: #4f46e5; /* Slightly darker indigo */
}

/* Ensure FileTable takes remaining space and scrolls */
.file-manager-content > :deep(.file-list-container) { /* Target FileTable's root */
  flex-grow: 1;
  overflow-y: auto; /* Enable vertical scroll within the table container */
  min-height: 0; /* Crucial for flex-grow + overflow-y */
}
</style>
