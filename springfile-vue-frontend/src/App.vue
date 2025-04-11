<template>
  <div id="app" class="app-layout">
    <Sidebar @upload-success="refreshFileList" />
    <main class="main-content">
      <FileTable ref="fileTableRef" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import Sidebar from './components/Sidebar.vue';
import FileTable from './components/FileTable.vue';

const fileTableRef = ref(null);

const refreshFileList = () => {
  if (fileTableRef.value) {
    fileTableRef.value.fetchFiles();
  } else {
    console.warn("FileTable component ref not available yet.");
  }
};

onMounted(() => {
});
</script>

<style scoped>
.app-layout {
  display: flex;
}

.main-content {
  flex-grow: 1; /* Takes remaining space */
  margin-left: 350px; /* Match updated sidebar width */
  padding: 0; /* Remove top/bottom padding */
  /* Removed height: 100vh and overflow-y: auto to allow natural page scroll */
}

</style>
