package com.example.springfile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the status of asynchronous tasks.
 * Uses ConcurrentHashMap for thread-safe storage.
 */
@Service
public class AsyncTaskManager {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskManager.class);

    // Stores the status of each task. Key: taskId, Value: AtomicReference containing the status object.
    // Using AtomicReference to allow atomic updates of the status object itself.
    private final Map<String, AtomicReference<TaskStatus>> taskStatuses = new ConcurrentHashMap<>();

    /**
     * Represents the status of a task. Can be extended with more fields like results, progress, etc.
     */
    public static class TaskStatus {
        private volatile String status; // e.g., "PROCESSING", "COMPLETED", "FAILED"
        private volatile String message; // Optional message or error details
        private volatile Map<Long, String> results; // Optional results map (e.g., from preprocessFiles)

        public TaskStatus(String initialStatus) {
            this.status = initialStatus;
        }

        // Getters (make them public)
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<Long, String> getResults() { return results; }

        // Setters (consider if needed, or update via updateStatus methods)
        // Using volatile ensures visibility across threads
    }

    /**
     * Registers a new task with an initial status.
     * @param taskId The unique ID for the task.
     * @param initialStatus The starting status (e.g., "PROCESSING").
     */
    public void registerTask(String taskId, String initialStatus) {
        if (taskStatuses.containsKey(taskId)) {
            logger.warn("Task ID {} already registered. Overwriting status.", taskId);
        }
        taskStatuses.put(taskId, new AtomicReference<>(new TaskStatus(initialStatus)));
        logger.info("Task {} registered with status: {}", taskId, initialStatus);
    }

    /**
     * Updates the status of an existing task.
     * @param taskId The ID of the task to update.
     * @param newStatus The new status string.
     * @param message Optional message (e.g., error details).
     * @param results Optional results map.
     */
    public void updateTaskStatus(String taskId, String newStatus, String message, Map<Long, String> results) {
        AtomicReference<TaskStatus> statusRef = taskStatuses.get(taskId);
        if (statusRef != null) {
            // Atomically update the TaskStatus object within the AtomicReference
            statusRef.updateAndGet(currentStatus -> {
                currentStatus.status = newStatus;
                currentStatus.message = message;
                currentStatus.results = results; // Store the results map
                return currentStatus;
            });
            logger.info("Task {} status updated to: {}", taskId, newStatus);
        } else {
            logger.warn("Attempted to update status for unknown or already completed task ID: {}", taskId);
        }
    }

     /**
     * Updates the status of an existing task (simplified version).
     * @param taskId The ID of the task to update.
     * @param newStatus The new status string.
     */
    public void updateTaskStatus(String taskId, String newStatus) {
        updateTaskStatus(taskId, newStatus, null, null);
    }

    /**
     * Retrieves the current status of a task.
     * @param taskId The ID of the task.
     * @return The TaskStatus object, or null if the task ID is not found.
     */
    public TaskStatus getTaskStatus(String taskId) {
        AtomicReference<TaskStatus> statusRef = taskStatuses.get(taskId);
        return (statusRef != null) ? statusRef.get() : null;
    }

    /**
     * Removes a task entry (optional, e.g., for cleanup).
     * @param taskId The ID of the task to remove.
     */
    public void removeTask(String taskId) {
        if (taskStatuses.remove(taskId) != null) {
            logger.info("Removed task entry for ID: {}", taskId);
        } else {
             logger.warn("Attempted to remove non-existent task entry for ID: {}", taskId);
        }
    }

    // TODO: Consider adding cleanup logic for old tasks (e.g., using a Scheduled task or TTL cache)
}
