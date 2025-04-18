<template>
  <div class="file-list-container">

    <!-- Processing Status Indicator -->
    <div v-if="processingStatus !== 'idle'" class="status-indicator" :class="`status-${processingStatus}`">
      <span>{{ processingMessage }}</span>
      <span v-if="processingStatus === 'done'" class="status-done"> ✔ Done</span>
      <button @click="dismissStatus" class="close-status-button" title="Dismiss status">×</button>
    </div>

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
      <button
        class="process-docx-button"
        @click="processSelectedDocxFiles"
        :disabled="selectedFileIds.size === 0 || !hasSelectedDocx"
        :title="hasSelectedDocx ? `Process ${selectedDocxCount} selected DOCX file(s)` : 'Select DOCX files to process'"
      >
        Process DOCX ({{ selectedDocxCount }})
      </button>
      <button
        class="embed-button"
        @click="triggerEmbedding"
        :disabled="selectedFileIds.size === 0"
        title="Trigger embedding for selected files"
      >
        Embed ({{ selectedFileIds.size }})
      </button>
    </div>

    <!-- Loading/Error State -->
    <div v-if="isLoadingFiles" class="loading-message">Loading files...</div>
    <div v-if="filesError" class="error-message">{{ filesError }}</div>

    <!-- Files Table Wrapper for Responsiveness -->
    <!-- Removed !isLoadingFiles and length check to prevent flicker/collapse during refresh -->
    <div class="table-wrapper" v-if="!filesError">
      <table class="files-table">
        <thead>
          <tr>
          <th>File Name</th>
          <th>Type</th>
          <th>Size</th>
          <th>Category</th>
          <th>Subcategory</th>
          <th>Uploaded At</th>
          <th>Embedding</th> <!-- Add Embedding Status Header -->
          <th>Preview</th> <!-- Add Preview Header -->
        </tr>
      </thead>
      <tbody v-if="filteredFiles.length > 0"> <!-- Revert to v-if, rely on wrapper min-height -->
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
          <td>{{ file.embedding ? 'Yes' : 'No' }}</td> <!-- Display Embedding Status -->
          <td class="preview-cell"> <!-- Add Preview Cell -->
            <span
              v-if="isFileTypePreviewable(file)"
              @click.stop="previewFile(file)"
              class="preview-icon"
              title="Preview file"
            >
              👁️
            </span>
            <span v-else class="preview-icon disabled" title="Preview not available for this file type">
              👁️
            </span>
          </td>
        </tr>
      </tbody>
      </table>
    </div>

<!-- No Files Message (Show only when not loading, no error, and the base list is actually empty) -->
<div v-if="!isLoadingFiles && !filesError && filesList.length === 0" class="no-files-message">
  {{ searchTerm ? 'No files match your search.' : 'No files uploaded yet.' }}
</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'; // Import nextTick

const filesList = ref([]);
const isLoadingFiles = ref(false);
const filesError = ref('');
const searchTerm = ref('');
const selectedFileIds = ref(new Set()); // Use a Set for efficient add/delete/has checks
const lastSelectedIndex = ref(-1); // For Shift+Click selection

// --- State for DOCX Processing Polling ---
const processingTaskId = ref(null);
const processingStatus = ref('idle'); // 'idle', 'processing', 'done', 'failed'
const processingMessage = ref('');
const pollingIntervalId = ref(null);
const POLLING_INTERVAL_MS = 3000; // Check status every 3 seconds


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

// Computed property to check if any selected file is a DOCX
const hasSelectedDocx = computed(() => {
  // Check fileType (more reliable) or fallback to fileName extension
  return selectedFiles.value.some(file =>
    file.fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
    (file.fileName && file.fileName.toLowerCase().endsWith('.docx'))
  );
});

// Computed property to count selected DOCX files for the button label
const selectedDocxCount = computed(() => {
  return selectedFiles.value.filter(file =>
    file.fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
    (file.fileName && file.fileName.toLowerCase().endsWith('.docx'))
  ).length;
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
  isLoadingFiles.value = true; // Keep loading indicator logic
  filesError.value = '';
  // Removed: filesList.value = []; // Don't clear the list prematurely

  try {
    const response = await fetch('/api/files');

    if (response.ok) {
      // Handle 204 No Content (empty list) or 200 OK (parse JSON)
      if (response.status !== 204) {
        const data = await response.json();
        // Basic check if data is an array before sorting
        const fetchedData = Array.isArray(data)
          ? data.sort((a, b) => new Date(b.uploadTimestamp) - new Date(a.uploadTimestamp))
          : [];
         if (!Array.isArray(data)) {
             console.warn("Received non-array data from /api/files, expected array:", data);
             // If data is invalid, keep the old list instead of clearing it
         } else {
             // Assign fetched data only if valid
             filesList.value = fetchedData;
         }
      } else {
          // Handle 204 No Content: Clear the list if it was previously populated
          filesList.value = [];
      }
    } else {
      // Handle non-OK responses (e.g., 500)
      // Don't clear the list on fetch error, keep showing old data
      throw new Error(`Failed to load files: ${response.status} ${response.statusText}`);
    }

  } catch (error) {
    console.error('Error fetching files:', error);
    filesError.value = `Error loading files: ${error.message}`;
    // Don't clear the list on fetch error
    // filesList.value = [];
  } finally {
    // Wait for the DOM to update after potential filesList change before hiding loading
    await nextTick();
    isLoadingFiles.value = false;
  }
};

defineExpose({
  fetchFiles
});

// --- Preview Logic ---

// Define previewable types
const NATIVE_PREVIEW_TYPES = [
  'image/jpeg', 'image/png', 'image/gif', 'image/svg+xml', 'image/webp',
  'application/pdf',
  // Removed text/plain unless specifically requested later
];
// Removed GOOGLE_DOCS_VIEWER_TYPES as they are not usable locally

// Helper to check if a file type is previewable natively by the browser
const isFileTypePreviewable = (file) => {
  const fileType = file.fileType || '';
  const fileName = file.fileName || '';
  // Prioritize MIME type if available
  if (fileType) {
    return NATIVE_PREVIEW_TYPES.includes(fileType);
  }
  // Fallback to extension for common types if MIME type is missing
  // Fallback to extension for common image/PDF types if MIME type is missing
  if (fileName.endsWith('.pdf')) return true;
  if (fileName.match(/\.(jpe?g|png|gif|svg|webp)$/i)) return true;
  // Removed fallback checks for DOCX, XLSX, PPTX, TXT

  return false; // Default to not previewable
};


const previewFile = (file) => {
  if (!isFileTypePreviewable(file)) {
    console.warn(`Preview attempt for non-previewable file: ${file.fileName}`);
    return;
  }

  const fileType = file.fileType || '';
  const fileName = file.fileName || ''; // Use for fallback check if needed
  const fileId = file.id;
  const backendViewUrl = `/api/files/view/${fileId}`;

  // Only handle native preview types now
  if (isFileTypePreviewable(file)) { // Re-check here for safety, though UI should prevent click
    console.log(`Opening native preview for: ${fileName}`);
    window.open(backendViewUrl, '_blank');
  } else {
    // This case should not be reached if the icon is correctly disabled
    console.warn(`Preview attempt for non-previewable file type: ${fileName} (Type: ${fileType})`);
    // Optionally show an alert, but the disabled icon is the primary feedback
    // alert('Preview is not available for this file type.');
  }
};


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


// --- Polling and Status Logic ---
const checkProcessingStatus = async (taskId) => {
  if (!taskId) return;
  console.log(`Polling status for task ID: ${taskId}`);
  try {
    // Corrected the fetch URL to include /api/files base path
    const response = await fetch(`/api/files/process/status/${taskId}`);
    if (!response.ok) {
      // Handle non-OK status check response (e.g., 404 if task ID is invalid/expired)
      console.error(`Status check failed for task ${taskId}: ${response.status}`);
      processingMessage.value = `Error checking status for task ${taskId}.`;
      processingStatus.value = 'failed';
      clearInterval(pollingIntervalId.value);
      pollingIntervalId.value = null;
      return;
    }

    const data = await response.json();
    console.log(`Status for task ${taskId}:`, data.status);

    // Backend should return status like 'PROCESSING', 'COMPLETED', 'FAILED'
    if (data.status === 'COMPLETED') {
      clearInterval(pollingIntervalId.value);
      pollingIntervalId.value = null;
      processingStatus.value = 'done';
      // Construct message based on results if available
      let doneMessage = "Processing complete.";
      if (data.results) {
          const processedCount = Object.values(data.results).filter(r => r === 'processed').length;
          const skippedCount = Object.values(data.results).filter(r => r === 'not_docx' || r === 'not_found').length;
          const errorCount = Object.values(data.results).filter(r => r.startsWith('error')).length;
          doneMessage = `Processing complete. Processed: ${processedCount}, Skipped/Not Found: ${skippedCount}, Errors: ${errorCount}.`;
          console.log("Detailed processing results:", data.results);
      }
      processingMessage.value = doneMessage;
      await fetchFiles(); // Refresh the table now that processing is done
    } else if (data.status === 'FAILED') {
      clearInterval(pollingIntervalId.value);
      pollingIntervalId.value = null;
      processingStatus.value = 'failed';
      processingMessage.value = `Processing failed: ${data.error || 'Unknown error'}`; // Assuming backend provides an 'error' field
      console.error(`Processing task ${taskId} failed:`, data.error);
    } else if (data.status === 'PROCESSING') {
      // Continue polling, message already set
    } else {
      // Unexpected status
      console.warn(`Unexpected status received for task ${taskId}: ${data.status}`);
      processingMessage.value = `Unexpected status: ${data.status}`;
      // Optionally stop polling on unexpected status
      // clearInterval(pollingIntervalId.value);
      // pollingIntervalId.value = null;
      // processingStatus.value = 'failed';
    }

  } catch (error) {
    console.error(`Error during status check for task ${taskId}:`, error);
    // Decide how to handle fetch errors during polling (e.g., stop polling, show error)
    processingMessage.value = `Error checking status: ${error.message}`;
    processingStatus.value = 'failed'; // Or keep polling? Depends on desired behavior.
    clearInterval(pollingIntervalId.value);
    pollingIntervalId.value = null;
  }
};

const startPolling = (taskId) => {
  if (pollingIntervalId.value) {
    clearInterval(pollingIntervalId.value); // Clear any existing interval
  }
  // Initial check immediately
  checkProcessingStatus(taskId);
  // Then set interval for subsequent checks
  pollingIntervalId.value = setInterval(() => checkProcessingStatus(taskId), POLLING_INTERVAL_MS);
};

const dismissStatus = () => {
  if (pollingIntervalId.value) {
    clearInterval(pollingIntervalId.value);
    pollingIntervalId.value = null;
  }
  processingStatus.value = 'idle';
  processingMessage.value = '';
  processingTaskId.value = null;
};


// --- Process DOCX Logic ---
const processSelectedDocxFiles = async () => {
  const idsToProcess = selectedFiles.value
    .filter(file =>
      file.fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
      (file.fileName && file.fileName.toLowerCase().endsWith('.docx'))
    )
    .map(file => file.id);

  if (idsToProcess.length === 0) {
    // Safeguard, though button should be disabled
    console.warn('Process DOCX clicked, but no eligible DOCX files are selected.');
    filesError.value = 'No DOCX files selected for processing.';
    return;
  }

  if (!window.confirm(`Are you sure you want to process ${idsToProcess.length} selected DOCX file(s)?`)) {
    return;
  }

  // Clear previous general errors and dismiss any old status messages
  filesError.value = '';
  dismissStatus(); // Clear previous polling state/message

  // --- Set status immediately after confirmation ---
  processingStatus.value = 'processing';
  processingMessage.value = `Initiating processing for ${idsToProcess.length} DOCX file(s)...`;
  // ---------------------------------------------

  try {
    console.log(`Sending request to initiate DOCX processing for IDs: ${idsToProcess}`);
    const response = await fetch('/api/files/process/docx', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(idsToProcess),
    });

    // --- Handle Initial Response ---
    if (response.status === 202) {
      // Accepted: Processing started, begin polling
      const responseData = await response.json(); // Expecting {"taskId": "..."}
      if (responseData.taskId) {
        processingTaskId.value = responseData.taskId;
        // Update message to include task ID, status is already 'processing'
        processingMessage.value = `Processing ${idsToProcess.length} DOCX file(s)... (Task ID: ${responseData.taskId.substring(0, 8)})`;
        startPolling(responseData.taskId);
      } else {
        // 202 but no taskId? Treat as an error.
        console.error('Processing accepted (202) but no taskId received.');
        processingStatus.value = 'failed';
        processingMessage.value = 'Processing started but failed to get status tracking ID.';
      }
    } else if (response.ok) {
        // Unexpected OK response (e.g., 200) - maybe backend is synchronous?
        console.warn('Received OK response instead of 202 Accepted. Assuming synchronous completion.');
        processingStatus.value = 'done'; // Assume done
        processingMessage.value = 'Processing completed (synchronously).';
        await fetchFiles(); // Refresh immediately
    } else {
      // Handle other non-OK responses (4xx, 5xx) for the initial request
      const errorData = await response.json().catch(() => ({})); // Try to parse error JSON
      console.error('DOCX processing initiation failed:', response.status, errorData);
      processingStatus.value = 'failed';
      processingMessage.value = `Failed to start processing: ${errorData.message || response.statusText}`;
      processingTaskId.value = null; // Ensure no task ID is stored
    }

  } catch (error) {
    // Handle fetch errors for the initial request
    console.error('Error initiating DOCX processing request:', error);
    processingStatus.value = 'failed';
    processingMessage.value = `Error starting processing: ${error.message}`;
    processingTaskId.value = null;
    if (pollingIntervalId.value) { // Ensure polling stops if fetch fails
        clearInterval(pollingIntervalId.value);
        pollingIntervalId.value = null;
    }
  }
  // Note: isLoadingFiles is no longer managed here as polling handles the duration
};


// --- Trigger Embedding Logic ---
const triggerEmbedding = async () => {
  const idsToEmbed = Array.from(selectedFileIds.value);
  if (idsToEmbed.length === 0) {
    console.warn('Embed button clicked with no selection.');
    return;
  }

  // Optional: Confirmation dialog
  // if (!window.confirm(`Are you sure you want to trigger embedding for ${idsToEmbed.length} file(s)?`)) {
  //   return;
  // }

  // Use the status indicator for feedback
  dismissStatus(); // Clear previous status
  processingStatus.value = 'processing'; // Use 'processing' state visually
  processingMessage.value = `Sending embedding request for ${idsToEmbed.length} file(s)...`;
  filesError.value = ''; // Clear general errors

  try {
    console.log(`Sending request to trigger embedding for IDs: ${idsToEmbed}`);
    const response = await fetch('/api/files/embed', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(idsToEmbed),
    });

    const responseData = await response.json().catch(() => ({})); // Try parsing JSON

    if (response.ok) {
      console.log('Embedding request successful:', responseData);
      processingStatus.value = 'done'; // Use 'done' state visually
      processingMessage.value = responseData.message || `Embedding request sent successfully for ${idsToEmbed.length} file(s).`;
      // Optionally clear selection after success
      // selectedFileIds.value.clear();
      // lastSelectedIndex.value = -1;
    } else {
      console.error('Embedding request failed:', response.status, responseData);
      processingStatus.value = 'failed'; // Use 'failed' state visually
      processingMessage.value = `Embedding request failed: ${responseData.message || response.statusText}`;
    }

  } catch (error) {
    console.error('Error sending embedding request:', error);
    processingStatus.value = 'failed'; // Use 'failed' state visually
    processingMessage.value = `Error sending embedding request: ${error.message}`;
  }
  // Note: We don't clear the selection here automatically, user can dismiss the status.
  // We also don't refresh the file list as the embedding happens in the background.
};


</script>

<style scoped>
.file-list-container {
  position: relative; /* Establish positioning context for absolute children */
  margin-top: 0; /* Removed top margin, handled by App.vue spacing */
  width: 95%;
/* Center the container both horizontally but top aligned */
  margin-left: auto;
  margin-right: auto;
  min-height: 50vh; /* Add min-height to prevent collapsing during refresh */
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

.process-docx-button {
  padding: 8px 15px;
  background-color: #28a745; /* Green color for process */
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.3s ease;
  white-space: nowrap;
}

.process-docx-button:hover:not(:disabled) {
  background-color: #218838; /* Darker green on hover */
}

.process-docx-button:disabled {
  background-color: #6c757d; /* Grey out when disabled */
  cursor: not-allowed;
  opacity: 0.65;
}

.embed-button {
  padding: 8px 15px;
  background-color: #6f42c1; /* Purple color for embedding */
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9em;
  transition: background-color 0.3s ease;
  white-space: nowrap;
}

.embed-button:hover:not(:disabled) {
  background-color: #5a32a3; /* Darker purple on hover */
}

.embed-button:disabled {
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
  min-height: 300px; /* Add minimum height to prevent collapse during refresh */
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

/* Make loading message overlay instead of pushing content */
.loading-message {
  position: absolute;
  top: 100px; /* Adjust as needed, below toolbar/status */
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(255, 255, 255, 0.8); /* Semi-transparent background */
  padding: 15px 25px;
  border-radius: 5px;
  box-shadow: 0 2px 5px rgba(0,0,0,0.2);
  z-index: 10; /* Ensure it's above other content */
  color: var(--color-text); /* Use text color */
  font-size: 0.95em;
  text-align: center;
  /* Remove margin-top as it's positioned absolutely */
}

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

/* --- Status Indicator Styles --- */
.status-indicator {
  padding: 10px 15px;
  margin-bottom: 15px; /* Space below the indicator */
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.9em;
  border: 1px solid transparent;
}

.status-processing {
  background-color: hsla(210, 100%, 70%, 0.15); /* Light blue background */
  border-color: hsla(210, 100%, 70%, 0.3);
  color: hsla(210, 80%, 45%, 1); /* Blue text */
}

.status-done {
  background-color: hsla(120, 60%, 70%, 0.15); /* Light green background */
  border-color: hsla(120, 60%, 70%, 0.3);
  color: hsla(120, 50%, 35%, 1); /* Green text */
}

.status-failed {
  background-color: hsla(0, 80%, 70%, 0.15); /* Light red background */
  border-color: hsla(0, 80%, 70%, 0.3);
  color: hsla(0, 70%, 45%, 1); /* Red text */
}

.status-done .status-done { /* Style the "✔ Done" text specifically */
  font-weight: bold;
  margin-left: 8px;
  color: hsla(120, 50%, 30%, 1); /* Darker green for emphasis */
}

.close-status-button {
  background: none;
  border: none;
  color: inherit; /* Inherit color from parent (.status-indicator) */
  font-size: 1.2em;
  font-weight: bold;
  cursor: pointer;
  padding: 0 5px;
  line-height: 1;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.close-status-button:hover {
  opacity: 1;
}

/* --- Preview Column Styles --- */
.preview-cell {
  text-align: center; /* Center the icon */
  width: 80px; /* Fixed width for the preview column */
}

.preview-icon {
  cursor: pointer;
  font-size: 1.3em; /* Make icon slightly larger */
  opacity: 0.8;
  transition: opacity 0.2s ease;
}

.preview-icon:hover {
  opacity: 1;
}

.preview-icon.disabled {
  cursor: not-allowed;
  opacity: 0.3;
}
</style>
