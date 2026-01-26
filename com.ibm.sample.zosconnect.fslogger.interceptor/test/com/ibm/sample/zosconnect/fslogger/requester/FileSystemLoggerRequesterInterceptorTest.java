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
package com.ibm.sample.zosconnect.fslogger.requester;

import com.ibm.zosconnect.spi.DataRequester;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for FileSystemLoggerRequesterInterceptor.
 * 
 * Tests interceptor lifecycle, configuration, and logging behavior for API Requester.
 * 
 * @author IBM
 */
class FileSystemLoggerRequesterInterceptorTest {

    @TempDir
    Path tempDir;

    @Mock
    private DataRequester mockData;

    private FileSystemLoggerRequesterInterceptor interceptor;
    private Map<String, Object> properties;
    private Map<Object, Object> requestStateMap;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        interceptor = new FileSystemLoggerRequesterInterceptor();
        requestStateMap = new HashMap<>();
        
        // Setup default properties
        properties = new HashMap<>();
        properties.put("id", "test-requester");
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

        assertEquals("FileSystemLoggerRequesterInterceptortest-requester", interceptor.getName());
        assertEquals(1, interceptor.getSequence());
    }

    @Test
    void testActivateWithNullProperties() {
        // Should handle null properties gracefully
        interceptor.activate(null, null);
        
        assertEquals("FileSystemLoggerRequesterInterceptornull", interceptor.getName());
    }

    @Test
    void testActivateWithMissingLogDirectory() {
        properties.remove("logDirectory");
        
        interceptor.activate(null, properties);
        
        assertEquals("FileSystemLoggerRequesterInterceptortest-requester", interceptor.getName());
    }

    @Test
    void testActivateWithOptionalParameters() {
        properties.put("requestHeaders", "Content-Type,X-Custom-Header");
        properties.put("responseHeaders", "Content-Type,X-Response-Id");
        properties.put("includeBody", true);
        
        interceptor.activate(null, properties);
        
        assertEquals("FileSystemLoggerRequesterInterceptortest-requester", interceptor.getName());
    }

    @Test
    void testPreInvokeRequesterCreatesLogEntry() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        // Verify log entry was created and stored
        assertTrue(requestStateMap.containsKey("FS_LOGGER_REQUESTER_ENTRY"));
        Object entry = requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
        assertNotNull(entry);
        assertTrue(entry instanceof LogEntryRequester);
    }

    @Test
    void testPreInvokeRequesterCapturesRequestHeaders() throws Exception {
        properties.put("requestHeaders", "Content-Type,Authorization");
        interceptor.activate(null, properties);
        
        Vector<String> headerNames = new Vector<>();
        headerNames.add("Content-Type");
        headerNames.add("Authorization");
        headerNames.add("X-Other-Header");
        when(mockData.getRequestHeaderNames()).thenReturn(headerNames.elements());
        when(mockData.getRequestHeader("Content-Type")).thenReturn("application/json");
        when(mockData.getRequestHeader("Authorization")).thenReturn("Bearer token");
        when(mockData.getRequestHeader("X-Other-Header")).thenReturn("value");
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        LogEntryRequester entry = (LogEntryRequester) requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
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
    void testPreInvokeRequesterCapturesRequestBody() throws Exception {
        properties.put("includeBody", true);
        interceptor.activate(null, properties);
        
        String requestBody = "{\"orderId\":\"12345\"}";
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(requestBody);
        
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        LogEntryRequester entry = (LogEntryRequester) requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
        String json = entry.toJson();
        
        assertTrue(json.contains("orderId"));
        assertTrue(json.contains("12345"));
    }

    @Test
    void testPreEndpointInvokeCapturesEndpointData() throws Exception {
        interceptor.activate(null, properties);
        
        // First create log entry
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        // Now test preEndpointInvoke
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/v1/customers");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        LogEntryRequester entry = (LogEntryRequester) requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
        String json = entry.toJson();
        
        assertTrue(json.contains("api.backend.com"));
        assertTrue(json.contains("443"));
        assertTrue(json.contains("/v1/customers"));
        assertTrue(json.contains("GET"));
    }

    @Test
    void testPostEndpointInvokeCapturesResponseCode() throws Exception {
        interceptor.activate(null, properties);
        
        // Setup log entry
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        // Test postEndpointInvoke
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        LogEntryRequester entry = (LogEntryRequester) requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
        String json = entry.toJson();
        
        assertTrue(json.contains("\"statusCode\":200"));
    }

    @Test
    void testPostInvokeRequesterWritesLogFile() throws Exception {
        interceptor.activate(null, properties);
        
        // Setup complete request flow
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.test.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        // Give it a moment to flush
        Thread.sleep(100);
        
        // Verify log file was created
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);
        
        // Verify log content
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("api.test.com"));
        assertTrue(content.contains("GET"));
        assertTrue(content.contains("\"statusCode\":200"));
    }

    @Test
    void testPostInvokeRequesterCapturesResponseData() throws Exception {
        properties.put("responseHeaders", "Content-Type");
        properties.put("includeBody", true);
        interceptor.activate(null, properties);
        
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        Vector<String> responseHeaders = new Vector<>();
        responseHeaders.add("Content-Type");
        when(mockData.getResponseHeaderNames()).thenReturn(responseHeaders.elements());
        when(mockData.getResponseHeader("Content-Type")).thenReturn("application/json");
        
        String responseBody = "{\"status\":\"success\"}";
        when(mockData.getOutputPayload()).thenReturn(responseBody);
        
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("Content-Type"));
        assertTrue(content.contains("application/json"));
        assertTrue(content.contains("success"));
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
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        // Deactivate should close file handler
        interceptor.deactivate(null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> interceptor.deactivate(null));
    }

    @Test
    void testPreInvokeRequesterWithoutActivation() throws Exception {
        // Don't activate interceptor
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        
        // Should not throw exception
        assertDoesNotThrow(() -> 
            interceptor.preInvokeRequester(requestStateMap, mockData)
        );
    }

    @Test
    void testCaseInsensitiveHeaderMatching() throws Exception {
        properties.put("requestHeaders", "content-type,authorization");
        interceptor.activate(null, properties);
        
        Vector<String> headerNames = new Vector<>();
        headerNames.add("Content-Type");  // Different case
        headerNames.add("AUTHORIZATION");  // Different case
        when(mockData.getRequestHeaderNames()).thenReturn(headerNames.elements());
        when(mockData.getRequestHeader("Content-Type")).thenReturn("application/json");
        when(mockData.getRequestHeader("AUTHORIZATION")).thenReturn("Bearer token");
        when(mockData.getInputPayload()).thenReturn(null);
        
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        LogEntryRequester entry = (LogEntryRequester) requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
        String json = entry.toJson();
        
        // Should match case-insensitively
        assertTrue(json.contains("Content-Type"));
        assertTrue(json.contains("AUTHORIZATION"));
    }

    @Test
    void testPreInvokeNotUsedForRequester() throws Exception {
        interceptor.activate(null, properties);
        
        // preInvoke should do nothing for requester
        assertDoesNotThrow(() -> 
            interceptor.preInvoke(requestStateMap, null, null)
        );
    }

    @Test
    void testPostInvokeNotUsedForRequester() throws Exception {
        interceptor.activate(null, properties);
        
        // postInvoke should do nothing for requester
        assertDoesNotThrow(() -> 
            interceptor.postInvoke(requestStateMap, null, null)
        );
    }

    @Test
    void testCompleteRequestFlow() throws Exception {
        properties.put("requestHeaders", "Content-Type");
        properties.put("responseHeaders", "Content-Type");
        properties.put("includeBody", true);
        interceptor.activate(null, properties);
        
        // 1. preInvokeRequester
        Vector<String> reqHeaders = new Vector<>();
        reqHeaders.add("Content-Type");
        when(mockData.getRequestHeaderNames()).thenReturn(reqHeaders.elements());
        when(mockData.getRequestHeader("Content-Type")).thenReturn("application/json");
        when(mockData.getInputPayload()).thenReturn("{\"request\":\"data\"}");
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        // 2. preEndpointInvoke
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.example.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/v1/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("POST");
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        // 3. postEndpointInvoke
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(201);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        // 4. postInvokeRequester
        Vector<String> respHeaders = new Vector<>();
        respHeaders.add("Content-Type");
        when(mockData.getResponseHeaderNames()).thenReturn(respHeaders.elements());
        when(mockData.getResponseHeader("Content-Type")).thenReturn("application/json");
        when(mockData.getOutputPayload()).thenReturn("{\"response\":\"success\"}");
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        // Verify complete log
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        assertTrue(logFiles.length > 0);
        
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("api.example.com"));
        assertTrue(content.contains("443"));
        assertTrue(content.contains("POST"));
        assertTrue(content.contains("/v1/test"));
        assertTrue(content.contains("request"));
        assertTrue(content.contains("data"));
        assertTrue(content.contains("\"statusCode\":201"));
        assertTrue(content.contains("response"));
        assertTrue(content.contains("success"));
    }

    @Test
    void testRequestSizeTracking() throws Exception {
        interceptor.activate(null, properties);
        
        String requestBody = "This is a test request body";
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(requestBody);
        
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        LogEntryRequester entry = (LogEntryRequester) requestStateMap.get("FS_LOGGER_REQUESTER_ENTRY");
        String json = entry.toJson();
        
        // Should track size even without includeBody
        assertTrue(json.contains("\"size\""));
    }

    @Test
    void testResponseSizeTracking() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        String responseBody = "This is a test response body";
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(responseBody);
        
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) -> 
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        
        // Should track size even without includeBody
        assertTrue(content.contains("\"size\""));
    }

    @Test
    void testQueryStringCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/search");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn("q=test&format=json");
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"queryString\":\"q=test&format=json\""));
    }

    @Test
    void testUserAndMappedUserCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.USER_NAME)).thenReturn("testuser");
        when(mockData.getData(DataRequester.USER_NAME_MAPPED)).thenReturn("TESTUSER");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"user\":\"testuser\""));
        assertTrue(content.contains("\"mappedUser\":\"TESTUSER\""));
    }

    @Test
    void testRequestTypeCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.REQUEST_APPLICATION_TYPE)).thenReturn("CICS");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"requestType\":\"CICS\""));
    }

    @Test
    void testZosInformationCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.MVS_JOBNAME)).thenReturn("TESTJOB");
        when(mockData.getData(DataRequester.MVS_JOBID)).thenReturn("JOB12345");
        when(mockData.getData(DataRequester.MVS_SYSNAME)).thenReturn("SYS1");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertFalse(content.contains("\"zos\""));
        assertTrue(content.contains("\"jobname\":\"TESTJOB\""));
        assertTrue(content.contains("\"jobid\":\"JOB12345\""));
        assertTrue(content.contains("\"sysname\":\"SYS1\""));
    }

    @Test
    void testCicsInformationCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.REQUEST_APPLICATION_TYPE)).thenReturn("CICS");
        when(mockData.getData(DataRequester.CICS_APPLID)).thenReturn("CICS01");
        when(mockData.getData(DataRequester.CICS_TASK_NUMBER)).thenReturn(12345);
        when(mockData.getData(DataRequester.CICS_TRANSID)).thenReturn("ABCD");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"cics\""));
        assertTrue(content.contains("\"applid\":\"CICS01\""));
        assertTrue(content.contains("\"taskNumber\":12345"));
        assertTrue(content.contains("\"transid\":\"ABCD\""));
    }

    @Test
    void testImsInformationCapture() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.REQUEST_APPLICATION_TYPE)).thenReturn("IMS");
        when(mockData.getData(DataRequester.IMS_IDENTIFIER)).thenReturn("IMS01");
        when(mockData.getData(DataRequester.IMS_REGION_ID)).thenReturn(2);
        when(mockData.getData(DataRequester.IMS_TRANSNAME)).thenReturn("IMSTRAN");
        when(mockData.getData(DataRequester.IMS_APPNAME)).thenReturn("IMSAPP");
        when(mockData.getData(DataRequester.IMS_PSBNAME)).thenReturn("IMSPSB");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertTrue(content.contains("\"ims\""));
        assertTrue(content.contains("\"identifier\":\"IMS01\""));
        assertTrue(content.contains("\"regionId\":2"));
        assertTrue(content.contains("\"transname\":\"IMSTRAN\""));
        assertTrue(content.contains("\"appname\":\"IMSAPP\""));
        assertTrue(content.contains("\"psbname\":\"IMSPSB\""));
    }

    @Test
    void testCicsInformationNotCapturedForNonCicsRequest() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.REQUEST_APPLICATION_TYPE)).thenReturn("IMS");
        when(mockData.getData(DataRequester.CICS_APPLID)).thenReturn("CICS01");
        when(mockData.getData(DataRequester.CICS_TASK_NUMBER)).thenReturn(12345);
        when(mockData.getData(DataRequester.CICS_TRANSID)).thenReturn("ABCD");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertFalse(content.contains("\"cics\""));
        assertFalse(content.contains("CICS01"));
    }

    @Test
    void testImsInformationNotCapturedForNonImsRequest() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.REQUEST_APPLICATION_TYPE)).thenReturn("CICS");
        when(mockData.getData(DataRequester.IMS_IDENTIFIER)).thenReturn("IMS01");
        when(mockData.getData(DataRequester.IMS_REGION_ID)).thenReturn(2);
        when(mockData.getData(DataRequester.IMS_TRANSNAME)).thenReturn("IMSTRAN");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("GET");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn(null);
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(200);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        assertFalse(content.contains("\"ims\""));
        assertFalse(content.contains("IMS01"));
    }

    @Test
    void testCompleteRequestWithAllNewFields() throws Exception {
        interceptor.activate(null, properties);
        
        when(mockData.getData(DataRequester.REQUEST_APPLICATION_TYPE)).thenReturn("CICS");
        when(mockData.getData(DataRequester.MVS_JOBNAME)).thenReturn("CICSJOB");
        when(mockData.getData(DataRequester.MVS_JOBID)).thenReturn("JOB00123");
        when(mockData.getData(DataRequester.MVS_SYSNAME)).thenReturn("SYSA");
        when(mockData.getData(DataRequester.CICS_APPLID)).thenReturn("CICS01");
        when(mockData.getData(DataRequester.CICS_TASK_NUMBER)).thenReturn(54321);
        when(mockData.getData(DataRequester.CICS_TRANSID)).thenReturn("ABCD");
        when(mockData.getData(DataRequester.USER_NAME)).thenReturn("testuser");
        when(mockData.getData(DataRequester.USER_NAME_MAPPED)).thenReturn("TESTUSER");
        when(mockData.getRequestHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getInputPayload()).thenReturn(null);
        interceptor.preInvokeRequester(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.ENDPOINT_HOST)).thenReturn("api.backend.com");
        when(mockData.getData(DataRequester.ENDPOINT_PORT)).thenReturn(443);
        when(mockData.getData(DataRequester.ENDPOINT_FULL_PATH)).thenReturn("/api/test");
        when(mockData.getData(DataRequester.ENDPOINT_METHOD)).thenReturn("POST");
        when(mockData.getData(DataRequester.ENDPOINT_QUERY_STRING)).thenReturn("param=value");
        interceptor.preEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getData(DataRequester.HTTP_RESPONSE_CODE)).thenReturn(201);
        interceptor.postEndpointInvoke(requestStateMap, mockData);
        
        when(mockData.getResponseHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(mockData.getOutputPayload()).thenReturn(null);
        interceptor.postInvokeRequester(requestStateMap, mockData);
        
        Thread.sleep(100);
        
        File[] logFiles = tempDir.toFile().listFiles((dir, name) ->
            name.startsWith("zosconnect-test-requester-") && name.endsWith(".log")
        );
        
        assertNotNull(logFiles);
        String content = readFileContent(logFiles[0]);
        
        // Verify all new fields are present
        assertTrue(content.contains("\"requestType\":\"CICS\""));
        assertFalse(content.contains("\"zos\""));
        assertTrue(content.contains("\"jobname\":\"CICSJOB\""));
        assertTrue(content.contains("\"jobid\":\"JOB00123\""));
        assertTrue(content.contains("\"sysname\":\"SYSA\""));
        assertTrue(content.contains("\"cics\""));
        assertTrue(content.contains("\"applid\":\"CICS01\""));
        assertTrue(content.contains("\"taskNumber\":54321"));
        assertTrue(content.contains("\"transid\":\"ABCD\""));
        assertTrue(content.contains("\"user\":\"testuser\""));
        assertTrue(content.contains("\"mappedUser\":\"TESTUSER\""));
        assertTrue(content.contains("\"statusCode\":201"));
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
