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
package com.ibm.sample.zosconnect.fslogger.provider;

import com.ibm.zosconnect.spi.Data;
import com.ibm.zosconnect.spi.HttpZosConnectRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for FileSystemLoggerProviderInterceptor.
 * 
 * Tests interceptor lifecycle, configuration, and logging behavior.
 * 
 * @author IBM
 */
class FileSystemLoggerProviderInterceptorTest {

    @TempDir
    Path tempDir;

    @Mock
    private HttpZosConnectRequest mockRequest;

    @Mock
    private Data mockData;

    @Mock
    private Principal mockPrincipal;

    private FileSystemLoggerProviderInterceptor interceptor;
    private Map<String, Object> properties;
    private Map<Object, Object> requestStateMap;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        interceptor = new FileSystemLoggerProviderInterceptor();
        requestStateMap = new HashMap<>();
        
        // Setup default properties
        properties = new HashMap<>();
        properties.put("id", "test-interceptor");
        properties.put("logDirectory", tempDir.toString());
        properties.put("maxFileSize", 10485760L);
        properties.put("maxFileCount", 10);
        properties.put("sequence", 1);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (interceptor != null) {
            interceptor.deactivate(null);
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testActivateWithValidConfiguration() {
        interceptor.activate(null, properties);

        assertEquals("FileSystemLoggerProviderInterceptortest-interceptor", interceptor.getName());
        assertEquals(1, interceptor.getSequence());
    }

    @Test
    void testActivateWithNullProperties() {
        // Should handle null properties gracefully
        interceptor.activate(null, null);
        
        // Interceptor should not be fully initialized
        assertEquals("FileSystemLoggerProviderInterceptornull", interceptor.getName());
    }

    @Test
    void testActivateWithMissingLogDirectory() {
        properties.remove("logDirectory");
        
        interceptor.activate(null, properties);
        
        // Should log error but not throw exception
        assertEquals("FileSystemLoggerProviderInterceptortest-interceptor", interceptor.getName());
    }

    @Test
    void testActivateWithInvalidLogDirectory() {
        properties.put("logDirectory", tempDir.resolve("nonexistent").toString());
        
        interceptor.activate(null, properties);
        
        // Should handle invalid directory gracefully
        assertEquals("FileSystemLoggerProviderInterceptortest-interceptor", interceptor.getName());
    }

    @Test
    void testActivateWithOptionalParameters() {
        properties.put("requestHeaders", "Content-Type,Authorization");
        properties.put("responseHeaders", "Content-Type,Cache-Control");
        properties.put("includeBody", true);
        
        interceptor.activate(null, properties);
        
        assertEquals("FileSystemLoggerProviderInterceptortest-interceptor", interceptor.getName());
    }

    @Test
    void testPreInvokeCreatesLogEntry() throws Exception {
        interceptor.activate(null, properties);
        
        // Setup mock request
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/customers");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockPrincipal.getName()).thenReturn("testuser");
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        
        // Verify log entry was created and stored
        assertTrue(requestStateMap.containsKey("FS_LOGGER_PROVIDER_ENTRY"));
        Object entry = requestStateMap.get("FS_LOGGER_PROVIDER_ENTRY");
        assertNotNull(entry);
        assertTrue(entry instanceof LogEntryProvider);
    }

    @Test
    void testPreInvokeCapturesRequestData() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/orders/123");
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockPrincipal.getName()).thenReturn("admin");
        when(mockRequest.getUserPrincipal()).thenReturn(mockPrincipal);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getData(Data.USER_NAME_MAPPED)).thenReturn("ADMIN");
        when(mockData.getInputPayload()).thenReturn("{\"orderId\":\"123\"}");
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        
        LogEntryProvider entry = (LogEntryProvider) requestStateMap.get("FS_LOGGER_PROVIDER_ENTRY");
        assertNotNull(entry);
        
        // Verify data was captured (check via JSON output)
        String json = entry.toJson();
        assertTrue(json.contains("/api/v1/orders/123"));
        assertTrue(json.contains("POST"));
    }

    @Test
    void testPreInvokeCapturesRequestHeaders() throws Exception {
        properties.put("requestHeaders", "Content-Type,Authorization");
        interceptor.activate(null, properties);
        
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        
        Vector<String> headerNames = new Vector<>();
        headerNames.add("Content-Type");
        headerNames.add("Authorization");
        headerNames.add("X-Other-Header");
        when(mockRequest.getHeaderNames()).thenReturn(headerNames.elements());
        when(mockRequest.getHeader("Content-Type")).thenReturn("application/json");
        when(mockRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(mockRequest.getHeader("X-Other-Header")).thenReturn("value");
        
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        
        LogEntryProvider entry = (LogEntryProvider) requestStateMap.get("FS_LOGGER_PROVIDER_ENTRY");
        String json = entry.toJson();
        
        // Should capture configured headers
        assertTrue(json.contains("Content-Type"));
        assertTrue(json.contains("application/json"));
        assertTrue(json.contains("Authorization"));
        assertTrue(json.contains("Bearer token"));
        // Should not capture non-configured headers
        assertFalse(json.contains("X-Other-Header"));
    }

    @Test
    void testPreSorInvokeCapturesSorData() throws Exception {
        interceptor.activate(null, properties);
        
        // First create log entry in preInvoke
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        
        // Now test preSorInvoke
        when(mockData.getData(Data.SOR_IDENTIFIER)).thenReturn("CICS01");
        when(mockData.getData(Data.SOR_RESOURCE)).thenReturn("CUSTOMER");
        when(mockData.getData(Data.SOR_REFERENCE)).thenReturn("REF123");
        
        interceptor.preSorInvoke(requestStateMap, mockRequest, mockData);
        
        LogEntryProvider entry = (LogEntryProvider) requestStateMap.get("FS_LOGGER_PROVIDER_ENTRY");
        String json = entry.toJson();
        
        assertTrue(json.contains("CICS01"));
        assertTrue(json.contains("CUSTOMER"));
        assertTrue(json.contains("REF123"));
    }

    @Test
    void testPostInvokeWritesLogFile() throws Exception {
        interceptor.activate(null, properties);
        
        // Setup complete request flow
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        when(mockData.getData(Data.HTTP_RESPONSE_CODE)).thenReturn(200);
        when(mockData.getData(Data.REQUEST_TIMED_OUT)).thenReturn(false);
        when(mockData.getMappedResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        interceptor.postInvoke(requestStateMap, mockRequest, mockData);
        
        // Give it a moment to flush
        Thread.sleep(100);
        
        // Verify log file was created
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-interceptor-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);
        
        // Verify log content
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("/api/test"));
        assertTrue(content.contains("GET"));
        assertTrue(content.contains("\"statusCode\":200"));
    }

    @Test
    void testModifiedUpdatesConfiguration() throws Exception {
        interceptor.activate(null, properties);
        
        // Modify configuration
        Map<String, Object> newProperties = new HashMap<>(properties);
        newProperties.put("sequence", 5);
        newProperties.put("requestHeaders", "X-Custom-Header");
        newProperties.put("includeBody", true);
        
        interceptor.modified(newProperties);
        
        assertEquals(5, interceptor.getSequence());
    }

    @Test
    void testDeactivateClosesResources() throws Exception {
        interceptor.activate(null, properties);
        
        // Write some data
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        
        // Deactivate should close file handler
        interceptor.deactivate(null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> interceptor.deactivate(null));
    }

    @Test
    void testPreInvokeWithoutActivation() throws Exception {
        // Don't activate interceptor
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> 
            interceptor.preInvoke(requestStateMap, mockRequest, mockData)
        );
    }

    @Test
    void testIncludeBodyConfiguration() throws Exception {
        properties.put("includeBody", true);
        interceptor.activate(null, properties);
        
        String requestBody = "{\"test\":\"data\"}";
        String responseBody = "{\"result\":\"success\"}";
        
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("POST");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(requestBody);
        when(mockData.getData(Data.HTTP_RESPONSE_CODE)).thenReturn(200);
        when(mockData.getData(Data.REQUEST_TIMED_OUT)).thenReturn(false);
        when(mockData.getMappedResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(responseBody);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        interceptor.postInvoke(requestStateMap, mockRequest, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-interceptor-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);
        
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("test"));
        assertTrue(content.contains("data"));
        assertTrue(content.contains("result"));
        assertTrue(content.contains("success"));
    }

    @Test
    void testCaseInsensitiveHeaderMatching() throws Exception {
        properties.put("requestHeaders", "content-type,authorization");
        interceptor.activate(null, properties);
        
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        
        Vector<String> headerNames = new Vector<>();
        headerNames.add("Content-Type");  // Different case
        headerNames.add("AUTHORIZATION");  // Different case
        when(mockRequest.getHeaderNames()).thenReturn(headerNames.elements());
        when(mockRequest.getHeader("Content-Type")).thenReturn("application/json");
        when(mockRequest.getHeader("AUTHORIZATION")).thenReturn("Bearer token");
        
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        
        LogEntryProvider entry = (LogEntryProvider) requestStateMap.get("FS_LOGGER_PROVIDER_ENTRY");
        String json = entry.toJson();
        
        // Should match case-insensitively
        assertTrue(json.contains("Content-Type"));
        assertTrue(json.contains("AUTHORIZATION"));
    }

    @Test
    void testTimedOutFlag() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockRequest.getRequestURI()).thenReturn("/api/test");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        when(mockData.getData(Data.HTTP_RESPONSE_CODE)).thenReturn(504);
        when(mockData.getData(Data.REQUEST_TIMED_OUT)).thenReturn(true);
        when(mockData.getMappedResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        interceptor.postInvoke(requestStateMap, mockRequest, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-interceptor-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"timedOut\":true"));
    }

    @Test
    void testQueryStringCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockRequest.getRequestURI()).thenReturn("/api/customers");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getQueryString()).thenReturn("filter=active&limit=10");
        when(mockRequest.getUserPrincipal()).thenReturn(null);
        when(mockRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        when(mockData.getData(Data.HTTP_RESPONSE_CODE)).thenReturn(200);
        when(mockData.getData(Data.REQUEST_TIMED_OUT)).thenReturn(false);
        when(mockData.getMappedResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        
        interceptor.preInvoke(requestStateMap, mockRequest, mockData);
        interceptor.postInvoke(requestStateMap, mockRequest, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-interceptor-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"queryString\":\"filter=active&limit=10\""));
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
