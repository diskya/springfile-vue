<template>
  <div class="home-view">
    <h1>Document Search</h1>
    <div class="search-section">
      <SearchBar @search="handleSearch" />
    </div>
    <!-- Display results only when a search has been performed -->
    <div v-if="searchResults.query || searchResults.loading || searchResults.error" class="results-section">
      <SearchResults
        :results="searchResults.results"
        :loading="searchResults.loading"
        :error="searchResults.error"
        :query="searchResults.query"
      />
    </div>
     <div v-else class="welcome-message">
      <p>Enter a query above to search through embedded documents.</p>
      <router-link to="/files" class="nav-link">Go to File Manager</router-link>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import SearchBar from '../components/SearchBar.vue';
import SearchResults from '../components/SearchResults.vue';

const searchResults = reactive({
  query: '',
  results: [],
  loading: false,
  error: null
});

// Function to handle the search using fetch
const handleSearch = async (query) => {
  console.log('HomeView searching for:', query);
  searchResults.loading = true;
  searchResults.error = null;
  searchResults.query = query; // Store the query
  searchResults.results = []; // Clear previous results

  try {
    const response = await fetch('/api/files/search', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        query: query,
        n_results: 10 // Request top 10 results
      }),
    });

    const responseData = await response.json();

    if (!response.ok) {
      console.error('Search failed with status:', response.status, responseData);
      const errorMessage = responseData.message || responseData.detail || `Search failed with status ${response.status}`;
      throw new Error(errorMessage);
    }

    console.log('Search response:', responseData);
    searchResults.results = responseData.results || [];

  } catch (err) {
    console.error('Search failed:', err);
    searchResults.error = err.message || 'An unknown error occurred during search.';
    searchResults.results = [];
  } finally {
    searchResults.loading = false;
  }
};
</script>

<style scoped>
.home-view {
  flex-grow: 1; /* Allow this view to take up available space from App.vue */
  display: flex;
  flex-direction: column;
  align-items: center; /* Center children horizontally */
  justify-content: flex-start; /* Align content towards the top */
  padding: 60px 20px 40px; /* More padding top */
  box-sizing: border-box;
  width: 100%; /* Ensure it takes full width */
  background: var(--color-background); /* Use theme background */
  text-align: center; /* Center text within children by default */
}

.content-wrapper {
  /* Optional: Add a wrapper if you need more control over the centered block */
  max-width: 900px; /* Max width for the main content block */
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}


h1 {
  font-size: 2.8rem; /* Slightly larger title */
  font-weight: 300; /* Lighter font weight */
  color: var(--color-heading); /* Use theme heading color */
  margin-bottom: 50px; /* Even more space below title */
  margin-top: 20px; /* Add some space above title */
}

.search-section {
  width: 100%;
  max-width: 700px; /* Slightly wider search */
  margin-bottom: 50px;
  /* Ensure SearchBar itself is centered if its internal alignment isn't center */
  display: flex;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); /* Slightly stronger shadow for visibility */
  background-color: var(--color-background-soft); /* Use theme soft background */
  padding: 20px; /* Add padding around the search bar */
  border-radius: 8px; /* Rounded corners */
  border: 1px solid var(--color-border); /* Add subtle border */
}

/* Style the SearchBar component itself if needed via deep selector */
.search-section :deep(.search-bar) {
  max-width: 100%; /* Ensure it doesn't overflow the section */
}
.search-section :deep(.search-input) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); /* Slightly adjusted shadow */
  border-color: #dee2e6;
  height: 45px; /* Increase height */
  font-size: 1rem;
}
.search-section :deep(.search-button) {
   box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
   height: 45px; /* Match input height */
   font-size: 1rem;
}


.results-section {
  width: 100%;
  max-width: 900px; /* Wider results */
  margin-top: 40px; /* More space above results */
  background-color: var(--color-background-soft); /* Use theme soft background */
  border-radius: 8px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1); /* Slightly stronger shadow */
  overflow: hidden;
  text-align: left; /* Align text left within results */
  border: 1px solid var(--color-border); /* Add subtle border */
}

/* Style the SearchResults component via deep selector */
.results-section :deep(.search-results) {
  border: none;
  background-color: transparent;
  padding: 20px 30px; /* Adjust padding */
}
.results-section :deep(h2) {
  font-size: 1.2rem; /* Adjust results heading size */
  font-weight: 500;
  margin-bottom: 20px;
}

.results-section :deep(.result-item) {
  box-shadow: none;
  border-bottom: 1px solid #f0f0f0; /* Lighter separator */
  margin-bottom: 0;
  padding: 20px 0;
  border-radius: 0;
}
.results-section :deep(.result-item:last-child) {
  border-bottom: none;
}


.welcome-message {
  margin-top: 30px;
  color: var(--color-text); /* Use theme text color */
  font-size: 1.1rem;
}

.nav-link {
  display: inline-block;
  margin-top: 20px; /* More space above link */
  padding: 10px 20px; /* Larger padding */
  background-color: var(--vt-c-indigo); /* Use theme indigo */
  color: var(--vt-c-white); /* Keep text white */
  text-decoration: none;
  border-radius: 5px; /* Slightly more rounded */
  transition: background-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
  font-weight: 500;
  border: 1px solid transparent; /* Add border for consistency */
}

.nav-link:hover {
  background-color: #3e526a; /* Slightly lighter indigo for hover */
  transform: translateY(-1px); /* Subtle lift effect */
  box-shadow: 0 2px 4px rgba(0,0,0,0.2); /* Add shadow on hover */
}
</style>
