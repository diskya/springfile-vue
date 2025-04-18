<template>
  <div id="app">
    <!-- RouterView will render the component based on the current route -->
    <router-view />
  </div>
</template>

<script setup>
// No specific logic needed here anymore as it's handled by the views
// Imports for Sidebar, FileTable, SearchBar, SearchResults, and related logic removed.
</script>

<style> /* Changed to non-scoped to target router-view container */
/* Global app styles */
html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; /* Example modern font stack */
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Ensure the div wrapping router-view can grow */
#app > div {
  flex-grow: 1;
  display: flex; /* Make the view container a flex container */
  flex-direction: column; /* Stack content vertically within the view container */
}

/* Remove styles that were specific to the old layout */
/* .app-layout, .main-content, .search-section, .results-section, .file-table-section */
/* These are now handled within FileManagerView.vue and HomeView.vue */
</style>
