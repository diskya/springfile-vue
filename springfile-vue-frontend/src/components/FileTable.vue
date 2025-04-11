<template>
  <div class="file-list-container">

    <!-- Toolbar: Search and Delete Button -->
    <div class="toolbar">
      <div class="search-container">
        <input type="text" v-model="searchTerm" placeholder="Search by name, category, or subcategory..." />
      </div>
      <button
        class="delete-button"
        @click="deleteSelectedFiles"
        :disabled="selectedFileIds.size === 0"
        title="Delete selected files"
      >
        Delete ({{ selectedFileIds.size }})
      </button>
      <button
        class="download-button"
        @click="handleDownload"
        :disabled="selectedFileIds.size === 0"
        :title="selectedFileIds.size > 0 ? `Download ${selectedFileIds.size} selected file(s)` : 'Select files to download'"
      >
        Download ({{ selectedFileIds.size }})
      </button>
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
        <tr
          v-for="(file, index) in filteredFiles"
          :key="file.id"
          @click="handleRowClick($event, file, index)"
          :class="{ 'selected-row': selectedFileIds.has(file.id) }"
          style="cursor: pointer;"
        >
          <td>{{ file.fileName }}</td>
          <td>{{ file.fileType }}</td>
          <td>{{ formatFileSize(file.size) }}</td>
          <td>{{ file.categoryName || 'N/A' }}</td>
          <td>{{ file.subcategoryName || '' }}</td>
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
const selectedFileIds = ref(new Set()); // Use a Set for efficient add/delete/has checks
const lastSelectedIndex = ref(-1); // For Shift+Click selection

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

// Computed property to get the actual file objects that are selected
const selectedFiles = computed(() => {
  return filesList.value.filter(file => selectedFileIds.value.has(file.id));
});

const formatFileSize = (bytes) => {
  if (bytes === undefined || bytes === null) return 'N/A';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
};

// Backend now provides the formatted string directly
const formatTimestamp = (timestampString) => {
  return timestampString || 'N/A';
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

// --- Selection Logic ---
const handleRowClick = (event, file, index) => {
  const fileId = file.id;
  const isCtrlPressed = event.ctrlKey || event.metaKey; // metaKey for Mac Command
  const isShiftPressed = event.shiftKey;

  if (isShiftPressed && lastSelectedIndex.value !== -1) {
    // Shift+Click: Select range
    const start = Math.min(lastSelectedIndex.value, index);
    const end = Math.max(lastSelectedIndex.value, index);
    // Determine if we are adding to or replacing the selection based on Ctrl key
    if (!isCtrlPressed) {
      selectedFileIds.value.clear(); // Clear existing selection if Ctrl is not pressed
    }
    // Iterate over the *currently filtered* list to select the range
    for (let i = start; i <= end; i++) {
       if (filteredFiles.value[i]) { // Ensure index is valid
         selectedFileIds.value.add(filteredFiles.value[i].id);
       }
    }
  } else if (isCtrlPressed) {
    // Ctrl+Click: Toggle selection
    if (selectedFileIds.value.has(fileId)) {
      selectedFileIds.value.delete(fileId);
    } else {
      selectedFileIds.value.add(fileId);
    }
    lastSelectedIndex.value = index; // Update last selected for potential Shift+Click next
  } else {
    // Simple Click: Toggle selection for the clicked row if it's the only one selected,
    // otherwise select only the clicked row.
    const wasSelected = selectedFileIds.value.has(fileId);
    const singleSelection = selectedFileIds.value.size === 1 && wasSelected;

    selectedFileIds.value.clear(); // Clear previous selection regardless

    if (!singleSelection) {
      // If it wasn't the only selected item, or wasn't selected at all, select it now.
      selectedFileIds.value.add(fileId);
    }
    // If it *was* the only selected item (singleSelection is true),
    // the clear() above effectively deselects it.

    lastSelectedIndex.value = index; // Update last selected index
  }
};

// --- Delete Logic ---
const deleteSelectedFiles = async () => {
  const idsToDelete = Array.from(selectedFileIds.value);
  if (idsToDelete.length === 0) {
    // Although button is disabled, add a safeguard
    console.warn('Delete button clicked with no selection.');
    return;
  }

  if (!window.confirm(`Are you sure you want to delete ${idsToDelete.length} file(s)? This action cannot be undone.`)) {
    return;
  }

  isLoadingFiles.value = true; // Indicate loading state during deletion
  filesError.value = '';

  try {
    const response = await fetch('/api/files/delete', {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(idsToDelete), // Send IDs in the request body
    });

    const responseData = await response.json(); // Always try to parse JSON response

    if (!response.ok) {
      // Handle non-OK responses (like 400, 500, or MULTI_STATUS 207)
      console.error('Deletion failed with status:', response.status, responseData);
      const errorCount = responseData.errors?.length || 0;
      const successCount = responseData.deletedFileIds?.length || 0;
      let errorMessage = `Deletion request failed: ${responseData.message || response.statusText}`;
      if (errorCount > 0) {
          const errorDetails = responseData.errors.map(e => `ID ${e.fileId}: ${e.error}`).join(', ');
          errorMessage = `Failed to delete ${errorCount} file(s). Details: ${errorDetails}`;
          if (successCount > 0) {
              errorMessage = `Partially successful: Deleted ${successCount} file(s). ${errorMessage}`;
          }
      }
      throw new Error(errorMessage);
    }

    // Handle successful response (OK 200)
    console.log('Deletion response:', responseData);
    let successMessage = `Successfully deleted ${responseData.deletedFileIds?.length || 0} file(s).`;
    // Optionally clear error message on full success
    filesError.value = '';
    console.log(successMessage);

  } catch (error) {
    console.error('Error during file deletion process:', error);
    // Display the caught error message (could be from fetch failure or thrown error)
    filesError.value = `Error deleting files: ${error.message}`;
  } finally {
    selectedFileIds.value.clear(); // Clear selection after attempt
    lastSelectedIndex.value = -1;
    await fetchFiles(); // Refresh the file list
    isLoadingFiles.value = false; // Reset loading state
  }
};


// --- Download Logic ---

// Helper function for single file download
const downloadSingleFile = async (fileId, fileName) => {
  const downloadUrl = `/api/files/download/${fileId}`;
  console.log(`Attempting to download single file: ${fileName} from ${downloadUrl}`);
  filesError.value = ''; // Clear previous errors

  try {
    const response = await fetch(downloadUrl);
    if (!response.ok) {
      let errorDetail = response.statusText;
      try {
        const errorData = await response.json();
        errorDetail = errorData.message || errorDetail;
      } catch (e) { /* Ignore if response is not JSON */ }
      throw new Error(`Download failed: ${response.status} ${errorDetail}`);
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    a.download = fileName || `download-${fileId}`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove();

  } catch (error) {
    console.error('Error downloading single file:', error);
    filesError.value = `Error downloading file: ${error.message}`;
  }
};

// Helper function for batch file download
const downloadBatch = async (fileIds) => {
  const downloadUrl = `/api/files/download/batch`;
  console.log(`Attempting to download batch of ${fileIds.length} files from ${downloadUrl}`);
  filesError.value = ''; // Clear previous errors

  try {
    const response = await fetch(downloadUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(fileIds),
    });

    if (!response.ok) {
      let errorDetail = response.statusText;
      try {
        // Attempt to parse error from backend, might not be JSON
        const errorText = await response.text();
        errorDetail = errorText || errorDetail;
      } catch (e) { /* Ignore parsing error */ }
      throw new Error(`Batch download failed: ${response.status} ${errorDetail}`);
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.style.display = 'none';
    a.href = url;
    // Generate a generic filename for the zip
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    a.download = `springfile-batch-${timestamp}.zip`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    a.remove();

  } catch (error) {
    console.error('Error downloading batch:', error);
    filesError.value = `Error downloading files: ${error.message}`;
  }
};

// Main download handler
const handleDownload = async () => {
  const idsToDownload = Array.from(selectedFileIds.value);
  const count = idsToDownload.length;

  if (count === 0) {
    console.warn('Download clicked with no selection.');
    return;
  }

  if (count === 1) {
    const fileId = idsToDownload[0];
    const selectedFile = filesList.value.find(f => f.id === fileId);
    if (selectedFile) {
      await downloadSingleFile(fileId, selectedFile.fileName);
    } else {
      filesError.value = `Error: Could not find details for selected file ID ${fileId}.`;
      console.error(`File details not found for ID: ${fileId}`);
    }
  } else {
    await downloadBatch(idsToDownload);
  }
};

</script>

<style scoped>
.file-list-container {
  margin-top: 0; /* Removed top margin, handled by App.vue spacing */
  width: 95%;
/* Center the container both horizontally but top aligned */
  margin-left: auto;
  margin-right: auto;

}

.file-list-container h2 {
  text-align: center;
  margin-bottom: 25px; /* Consistent margin */
  color: var(--color-heading); /* Use CSS variable */
  font-weight: 600;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 15px; /* Add space between search and button */
}

.search-container {
  flex-grow: 1; /* Allow search to take available space */
  margin-bottom: 0; /* Remove bottom margin as it's handled by toolbar */
}

.delete-button {
  padding: 8px 15px;
  background-color: #dc3545; /* Red color for delete */
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.3s ease;
  white-space: nowrap; /* Prevent button text wrapping */
}

.delete-button:hover:not(:disabled) {
  background-color: #c82333; /* Darker red on hover */
}

.delete-button:disabled {
  background-color: #6c757d; /* Grey out when disabled */
  cursor: not-allowed;
  opacity: 0.65;
}

.download-button {
  padding: 8px 15px;
  background-color: #007bff; /* Blue color for download */
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.3s ease;
  white-space: nowrap;
}

.download-button:hover:not(:disabled) {
  background-color: #0056b3; /* Darker blue on hover */
}

.download-button:disabled {
  background-color: #6c757d; /* Grey out when disabled */
  cursor: not-allowed;
  opacity: 0.65;
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

.files-table tbody tr.selected-row {
  background-color: hsla(210, 100%, 70%, 0.2); /* Light blue background for selected */
  /* border-left: 3px solid #007bff; */ /* Optional: Add a visual indicator */
}

.files-table tbody tr.selected-row:hover {
  background-color: hsla(210, 100%, 70%, 0.3); /* Slightly darker blue on hover when selected */
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
