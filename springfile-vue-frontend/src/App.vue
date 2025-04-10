<template>
  <div id="app">
    <header>
      <h1>SpringFile Manager</h1>
    </header>

    <main>
      <FileUpload @upload-success="refreshFileList" />
      <FileTable ref="fileTableRef" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'; 
import FileUpload from './components/FileUpload.vue';
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
/* Styles specific to App.vue */
/* #app is now styled globally in main.css */

header {
  width: 100%;
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 10px;
  /* Use CSS variables for consistency */
  border-bottom: 1px solid var(--color-border);
}

header h1 {
  color: var(--color-heading);
  font-weight: 600;
}

main {
  width: 100%;
  /* Max-width is handled globally now */
  padding: 0 15px; /* Keep horizontal padding */
}

/* Remove redundant styles now handled globally or unnecessary */
/*
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
*/
</style>
