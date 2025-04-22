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

<style> /* Global styles */
html, body {
  margin: 0;
  padding: 0;
  height: 100%; /* Ensure html and body take full height */
  width: 100%; /* Ensure html and body take full width */
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  /* Removed background-color: #f0f2f5; Let base.css handle theme background */
}

#app {
  /* min-height: 100vh; Removed to prevent unnecessary scrollbars */
  height: 100%; /* Rely on parent height */
  width: 100%; /* Ensure app takes full width */
  display: flex; /* Use flexbox for layout */
  flex-direction: column; /* Stack children vertically */
}

/* Target the direct child of #app which wraps <router-view> */
/* This assumes <router-view> is wrapped in a div or similar */
/* #app > * { */ /* Reverted: This caused layout issues in nested flex containers like FileManagerView */
  /* flex-grow: 1; */
  /* display: flex; */
  /* width: 100%; */
/* } */

/* Remove styles that were specific to the old layout */
/* .app-layout, .main-content, .search-section, .results-section, .file-table-section */
/* These are now handled within FileManagerView.vue and HomeView.vue */
</style>
