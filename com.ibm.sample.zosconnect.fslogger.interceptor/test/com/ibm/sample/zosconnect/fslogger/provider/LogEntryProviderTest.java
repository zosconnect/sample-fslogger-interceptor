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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for LogEntryProvider.
 * 
 * Tests JSON serialization and data capture for API Provider requests.
 * 
 * @author IBM
 */
class LogEntryProviderTest {

    private LogEntryProvider logEntry;

    @BeforeEach
    void setUp() {
        logEntry = new LogEntryProvider();
    }

    @Test
    void testBasicJsonSerialization() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setRequestUri("/api/v1/customers");
        logEntry.setRequestMethod("GET");
        logEntry.setResponseCode(200);

        String json = logEntry.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"response\""));
        assertTrue(json.contains("\"/api/v1/customers\""));
        assertTrue(json.contains("\"GET\""));
        assertTrue(json.contains("\"statusCode\":200"));
    }

    @Test
    void testTimestampFormatting() {
        // Set timestamp to a known value: 2009-02-13T23:31:30.000Z
        logEntry.setTimestamp(1234567890000L);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"timestamp\":\"2009-02-13T23:31:30.000Z\""));
    }

    @Test
    void testRequestInformation() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/v1/orders/123");
        logEntry.setRequestMethod("POST");
        logEntry.setUser("testuser");
        logEntry.setMappedUser("TESTUSER");
        logEntry.setRequestSize(256);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"uri\":\"/api/v1/orders/123\""));
        assertTrue(json.contains("\"method\":\"POST\""));
        assertTrue(json.contains("\"user\":\"testuser\""));
        assertTrue(json.contains("\"mappedUser\":\"TESTUSER\""));
        assertTrue(json.contains("\"size\":256"));
    }

    @Test
    void testRequestHeaders() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");
        logEntry.setRequestHeaders(headers);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"headers\""));
        assertTrue(json.contains("\"Content-Type\":\"application/json\""));
        assertTrue(json.contains("\"Authorization\":\"Bearer token123\""));
    }

    @Test
    void testRequestBody() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("POST");

        String requestBody = "{\"name\":\"John\",\"age\":30}";
        logEntry.setRequestBody(requestBody);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"body\""));
        assertTrue(json.contains("John"));
        assertTrue(json.contains("30"));
    }

    @Test
    void testSorInformation() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        logEntry.setSorIdentifier("CICS01");
        logEntry.setSorResource("CUSTOMER");
        logEntry.setSorReference("REF123");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"sor\""));
        assertTrue(json.contains("\"identifier\":\"CICS01\""));
        assertTrue(json.contains("\"resource\":\"CUSTOMER\""));
        assertTrue(json.contains("\"reference\":\"REF123\""));
    }

    @Test
    void testResponseInformation() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        logEntry.setResponseCode(201);
        logEntry.setResponseSize(512);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"statusCode\":201"));
        assertTrue(json.contains("\"size\":512"));
    }

    @Test
    void testResponseHeaders() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Cache-Control", "no-cache");
        logEntry.setResponseHeaders(headers);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"Content-Type\":\"application/json\""));
        assertTrue(json.contains("\"Cache-Control\":\"no-cache\""));
    }

    @Test
    void testResponseBody() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        String responseBody = "{\"status\":\"success\",\"data\":[1,2,3]}";
        logEntry.setResponseBody(responseBody);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"body\""));
        assertTrue(json.contains("success"));
        assertTrue(json.contains("[1,2,3]"));
    }

    @Test
    void testTimedOutFlag() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        logEntry.setTimedOut(true);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"timedOut\":true"));
    }

    @Test
    void testTimedOutFlagNotIncludedWhenFalse() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        logEntry.setTimedOut(false);

        String json = logEntry.toJson();

        assertFalse(json.contains("\"timedOut\""));
    }

    @Test
    void testJsonEscaping() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        // Test various characters that need escaping
        String bodyWithSpecialChars = "Line1\nLine2\tTabbed\"Quoted\"\\Backslash";
        logEntry.setRequestBody(bodyWithSpecialChars);

        String json = logEntry.toJson();

        assertTrue(json.contains("\\n"));  // Newline escaped
        assertTrue(json.contains("\\t"));  // Tab escaped
        assertTrue(json.contains("\\\""));  // Quote escaped
        assertTrue(json.contains("\\\\"));  // Backslash escaped
    }

    @Test
    void testNullValuesHandling() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        // Don't set optional fields

        String json = logEntry.toJson();

        // Should still produce valid JSON
        assertNotNull(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"response\""));
    }

    @Test
    void testCompleteLogEntry() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setRequestUri("/api/v1/customers/123");
        logEntry.setRequestMethod("PUT");
        logEntry.setUser("admin");
        logEntry.setMappedUser("ADMIN");

        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Content-Type", "application/json");
        reqHeaders.put("Accept", "application/json");
        logEntry.setRequestHeaders(reqHeaders);

        logEntry.setRequestBody("{\"name\":\"Updated Name\"}");
        logEntry.setRequestSize(100);

        logEntry.setSorIdentifier("CICS01");
        logEntry.setSorResource("CUSTOMER");
        logEntry.setSorReference("REF456");

        logEntry.setResponseCode(200);

        Map<String, String> respHeaders = new HashMap<>();
        respHeaders.put("Content-Type", "application/json");
        logEntry.setResponseHeaders(respHeaders);

        logEntry.setResponseBody("{\"status\":\"updated\"}");
        logEntry.setResponseSize(50);

        String json = logEntry.toJson();

        // Verify all components are present
        assertTrue(json.contains("\"timestamp\":\"2009-02-13T23:31:30.000Z\""));
        assertTrue(json.contains("\"uri\":\"/api/v1/customers/123\""));
        assertTrue(json.contains("\"method\":\"PUT\""));
        assertTrue(json.contains("\"user\":\"admin\""));
        assertTrue(json.contains("\"mappedUser\":\"ADMIN\""));
        assertTrue(json.contains("\"Content-Type\":\"application/json\""));
        assertTrue(json.contains("\"Accept\":\"application/json\""));
        assertTrue(json.contains("Updated Name"));
        assertTrue(json.contains("\"sor\""));
        assertTrue(json.contains("\"identifier\":\"CICS01\""));
        assertTrue(json.contains("\"statusCode\":200"));
        assertTrue(json.contains("updated"));
    }

    @Test
    void testToStringUsesToJson() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        String toStringResult = logEntry.toString();
        String toJsonResult = logEntry.toJson();

        assertEquals(toJsonResult, toStringResult);
    }

    @Test
    void testGetResponseCode() {
        assertNull(logEntry.getResponseCode());

        logEntry.setResponseCode(404);
        assertEquals(404, logEntry.getResponseCode());

        logEntry.setResponseCode(200);
        assertEquals(200, logEntry.getResponseCode());
    }

    @Test
    void testEmptyHeadersNotIncluded() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        Map<String, String> emptyHeaders = new HashMap<>();
        logEntry.setRequestHeaders(emptyHeaders);
        logEntry.setResponseHeaders(emptyHeaders);

        String json = logEntry.toJson();

        // Empty header maps should not appear in JSON
        int headersCount = json.split("\"headers\"").length - 1;
        assertEquals(0, headersCount);
    }

    @Test
    void testUnicodeCharacters() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        String unicodeBody = "Test with unicode: 日本語 中文 한국어 Ελληνικά 🎉";
        logEntry.setRequestBody(unicodeBody);

        String json = logEntry.toJson();

        assertTrue(json.contains("日本語"));
        assertTrue(json.contains("中文"));
        assertTrue(json.contains("한국어"));
        assertTrue(json.contains("Ελληνικά"));
    }

    @Test
    void testControlCharacterEscaping() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        // Test control characters
        String bodyWithControlChars = "Test\b\f\r\n\t";
        logEntry.setRequestBody(bodyWithControlChars);

        String json = logEntry.toJson();

        assertTrue(json.contains("\\b"));  // Backspace
        assertTrue(json.contains("\\f"));  // Form feed
        assertTrue(json.contains("\\r"));  // Carriage return
        assertTrue(json.contains("\\n"));  // Newline
        assertTrue(json.contains("\\t"));  // Tab
    }

    @Test
    void testRequestId() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        logEntry.setRequestId(123456789L);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"requestId\":123456789"));
    }

    @Test
    void testRequestIdNotIncludedWhenNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        // Don't set requestId

        String json = logEntry.toJson();

        assertFalse(json.contains("\"requestId\""));
    }

    @Test
    void testRequestIdPositionAfterTimestamp() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setRequestId(987654321L);
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");

        String json = logEntry.toJson();

        // Verify requestId comes after timestamp
        int timestampIndex = json.indexOf("\"timestamp\"");
        int requestIdIndex = json.indexOf("\"requestId\"");
        int requestIndex = json.indexOf("\"request\"");

        assertTrue(timestampIndex < requestIdIndex);
        assertTrue(requestIdIndex < requestIndex);
    }

    @Test
    void testQueryString() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/v1/customers");
        logEntry.setRequestMethod("GET");
        logEntry.setQueryString("filter=active&limit=10");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"queryString\":\"filter=active&limit=10\""));
    }

    @Test
    void testQueryStringNotIncludedWhenNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/v1/customers");
        logEntry.setRequestMethod("GET");
        // Don't set queryString

        String json = logEntry.toJson();

        assertFalse(json.contains("\"queryString\""));
    }

    @Test
    void testQueryStringWithSpecialCharacters() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestUri("/api/test");
        logEntry.setRequestMethod("GET");
        logEntry.setQueryString("name=John&age=30&city=New York");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"queryString\""));
        assertTrue(json.contains("name=John"));
        assertTrue(json.contains("age=30"));
        assertTrue(json.contains("New York"));
    }
}
