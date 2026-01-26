/*
 Copyright IBM Corporation 2026

 LICENSE: Apache License
          Version 2.0, January 2004
          http://www.apache.org/licenses/

 The following code is sample code created by IBM Corporation.
 This sample code is not part of any standard IBM product and
 is provided to you solely for the purpose of assisting you in
 the development of your applications.  The code is provided
 'as is', without warranty or condition of any kind.  IBM shall
 not be liable for any damages arising out of your use of the
 sample code, even if IBM has been advised of the possibility
 of such damages.
*/
package com.ibm.sample.zosconnect.fslogger.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * RollingFileHandler manages log files with automatic rotation based on file size.
 * 
 * Features:
 * - Automatic file rotation when maxFileSize is reached
 * - Maintains a maximum number of log files (deletes oldest when limit reached)
 * - Thread-safe operations
 * - Timestamped log file names
 * 
 * @author IBM
 */
public class RollingFileHandler {

    private static final String LOG_FILE_PREFIX = "zosconnect-";
    private static final String LOG_FILE_SUFFIX = ".log";
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");

    private final String id;
    private final String logDirectory;
    private final long maxFileSize;
    private final int maxFileCount;

    private File currentLogFile;
    private BufferedWriter currentWriter;
    private long currentFileSize;

    /**
     * Creates a new RollingFileHandler.
     *
     * @param id Unique identifier to include in log file names
     * @param logDirectory Directory where log files will be written
     * @param maxFileSize Maximum size of each log file in bytes
     * @param maxFileCount Maximum number of log files to retain
     * @throws IOException If the log directory cannot be created or accessed
     */
    public RollingFileHandler(String id, String logDirectory, long maxFileSize, int maxFileCount) throws IOException {
        this.id = id;
        this.logDirectory = logDirectory;
        this.maxFileSize = maxFileSize;
        this.maxFileCount = maxFileCount;

        // Validate log directory
        File logDir = new File(logDirectory);
        if (!logDir.exists()) {
            throw new IOException("Log directory path does not exist: " + logDirectory);
        }

        if (!logDir.isDirectory()) {
            throw new IOException("Log directory path is not a directory: " + logDirectory);
        }

        if (!logDir.canWrite()) {
            throw new IOException("Log directory is not writable: " + logDirectory);
        }

        // Initialize first log file
        rotateFile();
    }

    /**
     * Writes a log entry to the current log file.
     * 
     * Thread-safe method that handles automatic file rotation.
     * 
     * @param logEntry The log entry to write (should include newline if desired)
     * @throws IOException If writing fails
     */
    public synchronized void writeLog(String logEntry) throws IOException {
        if (currentWriter == null) {
            throw new IOException("RollingFileHandler is closed");
        }

        // Add newline if not present
        if (!logEntry.endsWith("\n")) {
            logEntry += "\n";
        }

        // Check if rotation is needed
        int entrySize = logEntry.getBytes("UTF-8").length;
        if (currentFileSize + entrySize > maxFileSize) {
            rotateFile();
        }

        // Write to current file
        currentWriter.write(logEntry);
        currentWriter.flush();
        currentFileSize += entrySize;
    }

    /**
     * Rotates to a new log file.
     * 
     * Closes the current file, creates a new one with a timestamp,
     * and deletes old files if the maximum count is exceeded.
     * 
     * @throws IOException If file operations fail
     */
    private synchronized void rotateFile() throws IOException {
        // Close current writer if exists
        if (currentWriter != null) {
            try {
                currentWriter.close();
            } catch (IOException e) {
                System.err.println("Error closing log file: " + e.getMessage());
            }
        }

        // Clean up old files if necessary
        cleanupOldFiles();

        // Create new log file with timestamp in format: <prefix>-<id>-<datetime>.log
        String timestamp = FILE_DATE_FORMAT.format(new Date());
        String fileName = LOG_FILE_PREFIX + id + "-" + timestamp + LOG_FILE_SUFFIX;
        currentLogFile = new File(logDirectory, fileName);

        // Handle file name collision (unlikely but possible)
        int counter = 1;
        while (currentLogFile.exists()) {
            fileName = LOG_FILE_PREFIX + id + "-" + timestamp + "-" + counter + LOG_FILE_SUFFIX;
            currentLogFile = new File(logDirectory, fileName);
            counter++;
        }

        // Create new writer
        currentWriter = new BufferedWriter(new FileWriter(currentLogFile, true));
        currentFileSize = 0;

        System.out.println("RollingFileHandler: Created new log file: " + currentLogFile.getAbsolutePath());
    }

    /**
     * Deletes old log files if the maximum count is exceeded.
     * 
     * Keeps only the most recent (maxFileCount - 1) files to make room for the new one.
     */
    private void cleanupOldFiles() {
        File logDir = new File(logDirectory);
        
        // Filter files that match this handler's ID: <prefix>-<id>-*.log
        String filePrefix = LOG_FILE_PREFIX + id + "-";
        File[] logFiles = logDir.listFiles((dir, name) ->
            name.startsWith(filePrefix) && name.endsWith(LOG_FILE_SUFFIX)
        );

        if (logFiles == null || logFiles.length < maxFileCount) {
            return; // No cleanup needed
        }

        // Sort files by last modified time (oldest first)
        Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));

        // Delete oldest files to make room for new one
        int filesToDelete = logFiles.length - maxFileCount + 1;
        for (int i = 0; i < filesToDelete && i < logFiles.length; i++) {
            if (logFiles[i].delete()) {
                System.out.println("RollingFileHandler: Deleted old log file: " + logFiles[i].getName());
            } else {
                System.err.println("RollingFileHandler: Failed to delete old log file: " + logFiles[i].getName());
            }
        }
    }

    /**
     * Closes the file handler and releases resources.
     * 
     * @throws IOException If closing the writer fails
     */
    public synchronized void close() throws IOException {
        if (currentWriter != null) {
            currentWriter.close();
            currentWriter = null;
        }
    }

    /**
     * Gets the current log file.
     * 
     * @return The current log file, or null if not initialized
     */
    public File getCurrentLogFile() {
        return currentLogFile;
    }

    /**
     * Gets the current file size in bytes.
     * 
     * @return The current file size
     */
    public long getCurrentFileSize() {
        return currentFileSize;
    }

    /**
     * Gets the log directory path.
     * 
     * @return The log directory path
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * Gets the maximum file size in bytes.
     * 
     * @return The maximum file size
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Gets the maximum number of log files to retain.
     * 
     * @return The maximum file count
     */
    public int getMaxFileCount() {
        return maxFileCount;
    }
}
