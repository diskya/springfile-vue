<template>
  <div class="search-results">
    <h2>Search Results for "{{ query }}"</h2>

    <div v-if="loading" class="loading-indicator">
      Loading results...
    </div>

    <div v-else-if="error" class="error-message">
      <p>Error fetching results:</p>
      <pre>{{ error }}</pre>
    </div>

    <div v-else-if="results && results.length > 0" class="results-list">
      <div v-for="result in results" :key="result.id" class="result-item">
        <div class="result-metadata">
          <span class="source">Source: {{ result.metadata?.source || 'N/A' }}</span>
          <span class="distance">Distance: {{ result.distance?.toFixed(4) || 'N/A' }}</span>
           <span class="chunk-index">Chunk: {{ result.metadata?.chunk_index ?? 'N/A' }}</span>
        </div>
        <p class="result-document">{{ result.document }}</p>
      </div>
    </div>

    <div v-else class="no-results">
      No results found for "{{ query }}".
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue';

const props = defineProps({
  results: {
    type: Array,
    required: true,
    default: () => []
  },
  loading: {
    type: Boolean,
    required: true,
    default: false
  },
  error: {
    type: [String, null],
    required: false,
    default: null
  },
  query: {
    type: String,
    required: true,
    default: ''
  }
});
</script>

<style scoped>
.search-results {
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.search-results h2 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 1.4rem;
  color: #333;
  border-bottom: 1px solid #eee;
  padding-bottom: 10px;
}

.loading-indicator,
.error-message,
.no-results {
  padding: 20px;
  text-align: center;
  color: #555;
}

.error-message {
  color: #d9534f; /* Bootstrap danger color */
  background-color: #f2dede;
  border: 1px solid #ebccd1;
  border-radius: 4px;
}

.error-message pre {
  white-space: pre-wrap; /* Wrap long error messages */
  word-wrap: break-word;
  text-align: left;
  margin-top: 10px;
  padding: 10px;
  background-color: #fff;
  border: 1px solid #ddd;
}

.results-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.result-item {
  border: 1px solid #ddd;
  background-color: #fff;
  padding: 15px;
  margin-bottom: 15px;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
}

.result-metadata {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 15px; /* Spacing between metadata items */
}

.result-metadata .source {
  font-weight: bold;
}

.result-document {
  font-size: 1rem;
  line-height: 1.6;
  color: #333;
  margin: 0; /* Remove default paragraph margin */
}
</style>
