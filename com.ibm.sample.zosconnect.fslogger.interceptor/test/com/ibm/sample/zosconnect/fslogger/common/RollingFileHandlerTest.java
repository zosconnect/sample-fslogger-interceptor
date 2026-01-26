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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for RollingFileHandler.
 * 
 * Tests file creation, rotation, cleanup, and thread safety.
 * 
 * @author IBM
 */
class RollingFileHandlerTest {

    @TempDir
    Path tempDir;

    private RollingFileHandler handler;

    @AfterEach
    void cleanup() throws IOException {
        if (handler != null) {
            handler.close();
        }
    }

    @Test
    void testConstructorCreatesLogFile() throws IOException {
        handler = new RollingFileHandler("test", tempDir.toString(), 1024, 5);
        
        assertNotNull(handler.getCurrentLogFile());
        assertTrue(handler.getCurrentLogFile().exists());
        assertTrue(handler.getCurrentLogFile().getName().startsWith("zosconnect-test-"));
        assertTrue(handler.getCurrentLogFile().getName().endsWith(".log"));
    }

    @Test
    void testConstructorThrowsExceptionForNonExistentDirectory() {
        String nonExistentDir = tempDir.resolve("nonexistent").toString();
        
        IOException exception = assertThrows(IOException.class, () -> {
            new RollingFileHandler("test", nonExistentDir, 1024, 5);
        });
        
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void testConstructorThrowsExceptionForFileInsteadOfDirectory() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        
        IOException exception = assertThrows(IOException.class, () -> {
            new RollingFileHandler("test", file.toString(), 1024, 5);
        });
        
        assertTrue(exception.getMessage().contains("not a directory"));
    }

    @Test
    void testWriteLogAddsNewlineIfMissing() throws IOException {
        handler = new RollingFileHandler("test", tempDir.toString(), 1024, 5);
        
        handler.writeLog("Test entry without newline");
        
        String content = readFileContent(handler.getCurrentLogFile());
        assertTrue(content.endsWith("\n"));
        assertEquals("Test entry without newline\n", content);
    }

    @Test
    void testWriteLogPreservesNewline() throws IOException {
        handler = new RollingFileHandler("test", tempDir.toString(), 1024, 5);
        
        handler.writeLog("Test entry with newline\n");
        
        String content = readFileContent(handler.getCurrentLogFile());
        assertEquals("Test entry with newline\n", content);
    }

    @Test
    void testWriteMultipleLogEntries() throws IOException {
        handler = new RollingFileHandler("test", tempDir.toString(), 1024, 5);
        
        handler.writeLog("Entry 1");
        handler.writeLog("Entry 2");
        handler.writeLog("Entry 3");
        
        String content = readFileContent(handler.getCurrentLogFile());
        String[] lines = content.split("\n");
        
        assertEquals(3, lines.length);
        assertEquals("Entry 1", lines[0]);
        assertEquals("Entry 2", lines[1]);
        assertEquals("Entry 3", lines[2]);
    }

    @Test
    void testFileRotationWhenMaxSizeExceeded() throws IOException {
        // Create handler with small max size (100 bytes)
        handler = new RollingFileHandler("test", tempDir.toString(), 100, 5);
        
        File firstFile = handler.getCurrentLogFile();
        
        // Write enough data to trigger rotation
        handler.writeLog("This is a long log entry that should trigger rotation when combined with others");
        handler.writeLog("Another long entry to ensure we exceed the 100 byte limit");
        
        File secondFile = handler.getCurrentLogFile();
        
        // Should have rotated to a new file
        assertNotEquals(firstFile.getName(), secondFile.getName());
        assertTrue(firstFile.exists());
        assertTrue(secondFile.exists());
    }

    @Test
    void testMaxFileCountEnforcement() throws IOException {
        // Create handler with max 3 files
        handler = new RollingFileHandler("test", tempDir.toString(), 50, 3);
        
        // Force creation of multiple files by writing data that exceeds size limit
        for (int i = 0; i < 5; i++) {
            handler.writeLog("Entry " + i + " - This is a long entry to trigger rotation");
        }
        
        // Count log files for this handler
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        // Should have at most 3 files (maxFileCount)
        assertTrue(logFiles.length <= 3, "Expected at most 3 files, but found " + logFiles.length);
    }

    @Test
    void testMultipleHandlersWithDifferentIds() throws IOException {
        RollingFileHandler handler1 = new RollingFileHandler("handler1", tempDir.toString(), 1024, 5);
        RollingFileHandler handler2 = new RollingFileHandler("handler2", tempDir.toString(), 1024, 5);
        
        try {
            handler1.writeLog("Entry from handler1");
            handler2.writeLog("Entry from handler2");
            
            assertTrue(handler1.getCurrentLogFile().getName().contains("handler1"));
            assertTrue(handler2.getCurrentLogFile().getName().contains("handler2"));
            
            String content1 = readFileContent(handler1.getCurrentLogFile());
            String content2 = readFileContent(handler2.getCurrentLogFile());
            
            assertTrue(content1.contains("handler1"));
            assertTrue(content2.contains("handler2"));
        } finally {
            handler1.close();
            handler2.close();
        }
    }

    @Test
    void testCloseReleasesResources() throws IOException {
        handler = new RollingFileHandler("test", tempDir.toString(), 1024, 5);
        
        handler.writeLog("Test entry");
        handler.close();
        
        // Writing after close should throw exception
        IOException exception = assertThrows(IOException.class, () -> {
            handler.writeLog("This should fail");
        });
        
        assertTrue(exception.getMessage().contains("closed"));
    }

    @Test
    void testGetters() throws IOException {
        String logDir = tempDir.toString();
        long maxSize = 2048;
        int maxCount = 7;
        
        handler = new RollingFileHandler("test", logDir, maxSize, maxCount);
        
        assertEquals(logDir, handler.getLogDirectory());
        assertEquals(maxSize, handler.getMaxFileSize());
        assertEquals(maxCount, handler.getMaxFileCount());
        assertEquals(0, handler.getCurrentFileSize());
    }

    @Test
    void testCurrentFileSizeTracking() throws IOException {
        handler = new RollingFileHandler("test", tempDir.toString(), 1024, 5);
        
        assertEquals(0, handler.getCurrentFileSize());
        
        String entry = "Test entry";
        handler.writeLog(entry);
        
        // Size should include the entry plus newline
        int expectedSize = (entry + "\n").getBytes("UTF-8").length;
        assertEquals(expectedSize, handler.getCurrentFileSize());
    }

    @Test
    void testThreadSafety() throws Exception {
        handler = new RollingFileHandler("test", tempDir.toString(), 10240, 5);
        
        int threadCount = 10;
        int entriesPerThread = 100;
        List<Thread> threads = new ArrayList<>();
        
        // Create multiple threads writing concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < entriesPerThread; j++) {
                        handler.writeLog("Thread " + threadId + " Entry " + j);
                    }
                } catch (IOException e) {
                    fail("Thread " + threadId + " failed: " + e.getMessage());
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Count total entries across all log files
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        
        int totalEntries = 0;
        for (File logFile : logFiles) {
            String content = readFileContent(logFile);
            String[] lines = content.split("\n");
            totalEntries += lines.length;
        }
        
        // Should have all entries written
        assertEquals(threadCount * entriesPerThread, totalEntries);
    }

    @Test
    void testFileNameCollisionHandling() throws IOException {
        // This test verifies that if a file with the same timestamp exists,
        // the handler adds a counter to avoid collision
        handler = new RollingFileHandler("test", tempDir.toString(), 50, 5);
        
        // Force multiple rotations in quick succession
        for (int i = 0; i < 3; i++) {
            handler.writeLog("Entry " + i + " - Force rotation with long text");
        }
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 1);
        
        // All files should have unique names
        List<String> fileNames = new ArrayList<>();
        for (File file : logFiles) {
            assertFalse(fileNames.contains(file.getName()), 
                "Duplicate file name found: " + file.getName());
            fileNames.add(file.getName());
        }
    }

    /**
     * Helper method to read file content.
     */
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
