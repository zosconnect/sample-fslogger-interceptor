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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for LogEntryRequester.
 * 
 * Tests JSON serialization and data capture for API Requester requests.
 * 
 * @author IBM
 */
class LogEntryRequesterTest {

    private LogEntryRequester logEntry;

    @BeforeEach
    void setUp() {
        logEntry = new LogEntryRequester();
    }

    @Test
    void testBasicJsonSerialization() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setEndpointHost("api.example.com");
        logEntry.setEndpointPort(443);
        logEntry.setEndpointMethod("GET");
        logEntry.setEndpointPath("/v1/customers");
        logEntry.setResponseStatusCode(200);

        String json = logEntry.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"endpoint\""));
        assertTrue(json.contains("\"response\""));
        assertTrue(json.contains("\"api.example.com\""));
        assertTrue(json.contains("\"port\":443"));
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
    void testEndpointInformation() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setEndpointHost("backend.example.com");
        logEntry.setEndpointPort(8080);
        logEntry.setEndpointMethod("POST");
        logEntry.setEndpointPath("/api/orders");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"endpoint\""));
        assertTrue(json.contains("\"host\":\"backend.example.com\""));
        assertTrue(json.contains("\"port\":8080"));
        assertTrue(json.contains("\"method\":\"POST\""));
        assertTrue(json.contains("\"path\":\"/api/orders\""));
    }

    @Test
    void testRequestHeaders() {
        logEntry.setTimestamp(System.currentTimeMillis());

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "custom-value");
        logEntry.setRequestHeaders(headers);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"headers\""));
        assertTrue(json.contains("\"Content-Type\":\"application/json\""));
        assertTrue(json.contains("\"X-Custom-Header\":\"custom-value\""));
    }

    @Test
    void testRequestBody() {
        logEntry.setTimestamp(System.currentTimeMillis());

        String requestBody = "{\"orderId\":\"12345\",\"amount\":99.99}";
        logEntry.setRequestBody(requestBody);
        logEntry.setRequestSize(100);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"body\""));
        assertTrue(json.contains("orderId"));
        assertTrue(json.contains("12345"));
        assertTrue(json.contains("\"size\":100"));
    }

    @Test
    void testResponseHeaders() {
        logEntry.setTimestamp(System.currentTimeMillis());

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Cache-Control", "max-age=3600");
        logEntry.setResponseHeaders(headers);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"Content-Type\":\"application/json\""));
        assertTrue(json.contains("\"Cache-Control\":\"max-age=3600\""));
    }

    @Test
    void testResponseBody() {
        logEntry.setTimestamp(System.currentTimeMillis());

        String responseBody = "{\"status\":\"success\",\"orderId\":\"12345\"}";
        logEntry.setResponseBody(responseBody);
        logEntry.setResponseSize(200);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"body\""));
        assertTrue(json.contains("success"));
        assertTrue(json.contains("12345"));
        assertTrue(json.contains("\"size\":200"));
    }

    @Test
    void testResponseStatusCode() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setResponseStatusCode(201);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"statusCode\":201"));
    }

    @Test
    void testGetResponseStatusCode() {
        assertNull(logEntry.getResponseStatusCode());

        logEntry.setResponseStatusCode(404);
        assertEquals(404, logEntry.getResponseStatusCode());

        logEntry.setResponseStatusCode(200);
        assertEquals(200, logEntry.getResponseStatusCode());
    }

    @Test
    void testJsonEscaping() {
        logEntry.setTimestamp(System.currentTimeMillis());

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
        logEntry.setEndpointHost("api.backend.com");
        logEntry.setEndpointPort(443);
        logEntry.setEndpointMethod("PUT");
        logEntry.setEndpointPath("/v2/customers/123");

        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("Content-Type", "application/json");
        reqHeaders.put("Authorization", "Bearer token123");
        logEntry.setRequestHeaders(reqHeaders);

        logEntry.setRequestBody("{\"name\":\"Updated Customer\"}");
        logEntry.setRequestSize(150);

        logEntry.setResponseStatusCode(200);

        Map<String, String> respHeaders = new HashMap<>();
        respHeaders.put("Content-Type", "application/json");
        respHeaders.put("X-Request-Id", "req-456");
        logEntry.setResponseHeaders(respHeaders);

        logEntry.setResponseBody("{\"status\":\"updated\",\"id\":\"123\"}");
        logEntry.setResponseSize(75);

        String json = logEntry.toJson();

        // Verify all components are present
        assertTrue(json.contains("\"timestamp\":\"2009-02-13T23:31:30.000Z\""));
        assertTrue(json.contains("\"host\":\"api.backend.com\""));
        assertTrue(json.contains("\"port\":443"));
        assertTrue(json.contains("\"method\":\"PUT\""));
        assertTrue(json.contains("\"path\":\"/v2/customers/123\""));
        assertTrue(json.contains("\"Content-Type\":\"application/json\""));
        assertTrue(json.contains("\"Authorization\":\"Bearer token123\""));
        assertTrue(json.contains("Updated Customer"));
        assertTrue(json.contains("\"statusCode\":200"));
        assertTrue(json.contains("\"X-Request-Id\":\"req-456\""));
        assertTrue(json.contains("updated"));
    }

    @Test
    void testToStringUsesToJson() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setEndpointHost("test.com");
        logEntry.setEndpointMethod("GET");

        String toStringResult = logEntry.toString();
        String toJsonResult = logEntry.toJson();

        assertEquals(toJsonResult, toStringResult);
    }

    @Test
    void testEmptyHeadersNotIncluded() {
        logEntry.setTimestamp(System.currentTimeMillis());

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
    void testEndpointWithoutPort() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setEndpointHost("api.example.com");
        logEntry.setEndpointMethod("GET");
        logEntry.setEndpointPath("/api/test");
        // Don't set port

        String json = logEntry.toJson();

        assertTrue(json.contains("\"host\":\"api.example.com\""));
        assertTrue(json.contains("\"method\":\"GET\""));
        assertTrue(json.contains("\"path\":\"/api/test\""));
        assertFalse(json.contains("\"port\""));
    }

    @Test
    void testMinimalLogEntry() {
        logEntry.setTimestamp(System.currentTimeMillis());

        String json = logEntry.toJson();

        // Should have basic structure even with minimal data
        assertTrue(json.contains("\"timestamp\""));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"response\""));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    void testRequestWithoutBody() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setEndpointHost("api.example.com");
        logEntry.setEndpointMethod("GET");
        logEntry.setEndpointPath("/api/test");
        // Don't set request body

        String json = logEntry.toJson();

        assertTrue(json.contains("\"endpoint\""));
        assertFalse(json.contains("\"body\""));
    }

    @Test
    void testResponseWithoutBody() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setResponseStatusCode(204); // No Content
        // Don't set response body

        String json = logEntry.toJson();

        assertTrue(json.contains("\"statusCode\":204"));
        // Response body should not be in JSON if not set
        int bodyCount = json.split("\"body\"").length - 1;
        assertEquals(0, bodyCount);
    }

    @Test
    void testMultipleHeadersInCorrectFormat() {
        logEntry.setTimestamp(System.currentTimeMillis());

        Map<String, String> headers = new HashMap<>();
        headers.put("Header1", "Value1");
        headers.put("Header2", "Value2");
        headers.put("Header3", "Value3");
        logEntry.setRequestHeaders(headers);

        String json = logEntry.toJson();

        // All headers should be present
        assertTrue(json.contains("\"Header1\":\"Value1\""));
        assertTrue(json.contains("\"Header2\":\"Value2\""));
        assertTrue(json.contains("\"Header3\":\"Value3\""));
    }

    @Test
    void testRequestId() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestId(123456789L);

        String json = logEntry.toJson();

        assertTrue(json.contains("\"requestId\":123456789"));
    }

    @Test
    void testRequestIdNotIncludedWhenNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        // Don't set requestId

        String json = logEntry.toJson();

        assertFalse(json.contains("\"requestId\""));
    }

    @Test
    void testRequestIdPositionAfterTimestamp() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setRequestId(987654321L);

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
        logEntry.setEndpointHost("api.example.com");
        logEntry.setEndpointMethod("GET");
        logEntry.setEndpointPath("/api/customers");
        logEntry.setQueryString("format=json&include=details");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"queryString\":\"format=json&include=details\""));
    }

    @Test
    void testQueryStringNotIncludedWhenNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setEndpointHost("api.example.com");
        logEntry.setEndpointMethod("GET");
        logEntry.setEndpointPath("/api/customers");
        // Don't set queryString

        String json = logEntry.toJson();

        assertFalse(json.contains("\"queryString\""));
    }

    @Test
    void testQueryStringWithSpecialCharacters() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setEndpointHost("api.example.com");
        logEntry.setEndpointPath("/api/search");
        logEntry.setQueryString("q=test&filter=active&sort=name");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"queryString\""));
        assertTrue(json.contains("q=test"));
        assertTrue(json.contains("filter=active"));
        assertTrue(json.contains("sort=name"));
    }

    @Test
    void testUserInformation() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setUser("testuser");
        logEntry.setMappedUser("TESTUSER");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"user\":\"testuser\""));
        assertTrue(json.contains("\"mappedUser\":\"TESTUSER\""));
    }

    @Test
    void testUserNotIncludedWhenNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        // Don't set user or mappedUser

        String json = logEntry.toJson();

        assertFalse(json.contains("\"user\""));
        assertFalse(json.contains("\"mappedUser\""));
    }

    @Test
    void testUserAndMappedUserInRequestSection() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestId(123L);
        logEntry.setUser("admin");
        logEntry.setMappedUser("ADMIN");
        logEntry.setRequestBody("{\"test\":\"data\"}");

        String json = logEntry.toJson();

        int requestIdIndex = json.indexOf("\"requestId\"");
        int userIndex = json.indexOf("\"user\"");
        int mappedUserIndex = json.indexOf("\"mappedUser\"");
        int requestIndex = json.indexOf("\"request\"");

        assertTrue(requestIdIndex < userIndex);
        assertTrue(requestIdIndex < mappedUserIndex);
        assertTrue(userIndex < requestIndex);
        assertTrue(mappedUserIndex < requestIndex);
    }

    @Test
    void testRequestType() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("CICS");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"requestType\":\"CICS\""));
    }

    @Test
    void testRequestTypeNotIncludedWhenNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        // Don't set requestType

        String json = logEntry.toJson();

        assertFalse(json.contains("\"requestType\""));
    }

    @Test
    void testZosInformation() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setMvsJobname("TESTJOB");
        logEntry.setMvsJobid("JOB12345");
        logEntry.setMvsSysname("SYS1");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"request\""));
        assertFalse(json.contains("\"zos\""));
        assertTrue(json.contains("\"jobname\":\"TESTJOB\""));
        assertTrue(json.contains("\"jobid\":\"JOB12345\""));
        assertTrue(json.contains("\"sysname\":\"SYS1\""));
    }

    @Test
    void testZosInformationPartial() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setMvsJobname("TESTJOB");
        // Don't set jobid or sysname

        String json = logEntry.toJson();

        assertTrue(json.contains("\"request\""));
        assertFalse(json.contains("\"zos\""));
        assertTrue(json.contains("\"jobname\":\"TESTJOB\""));
        assertFalse(json.contains("\"jobid\""));
        assertFalse(json.contains("\"sysname\""));
    }

    @Test
    void testCicsInformationWhenRequestTypeIsCics() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("CICS");
        logEntry.setCicsApplid("CICS01");
        logEntry.setCicsTaskNumber(12345);
        logEntry.setCicsTransid("TRN1");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"cics\""));
        assertTrue(json.contains("\"applid\":\"CICS01\""));
        assertTrue(json.contains("\"taskNumber\":12345"));
        assertTrue(json.contains("\"transid\":\"TRN1\""));
    }

    @Test
    void testCicsInformationNotIncludedWhenRequestTypeIsNotCics() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("IMS");
        logEntry.setCicsApplid("CICS01");
        logEntry.setCicsTaskNumber(12345);
        logEntry.setCicsTransid("TRN1");

        String json = logEntry.toJson();

        assertFalse(json.contains("\"cics\""));
        assertFalse(json.contains("CICS01"));
    }

    @Test
    void testCicsInformationNotIncludedWhenRequestTypeIsNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        // Don't set requestType
        logEntry.setCicsApplid("CICS01");
        logEntry.setCicsTaskNumber(12345);
        logEntry.setCicsTransid("TRN1");

        String json = logEntry.toJson();

        assertFalse(json.contains("\"cics\""));
    }

    @Test
    void testCicsInformationPartial() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("CICS");
        logEntry.setCicsApplid("CICS01");
        // Don't set taskNumber or transid

        String json = logEntry.toJson();

        assertTrue(json.contains("\"cics\""));
        assertTrue(json.contains("\"applid\":\"CICS01\""));
        assertFalse(json.contains("\"taskNumber\""));
        assertFalse(json.contains("\"transid\""));
    }

    @Test
    void testImsInformationWhenRequestTypeIsIms() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("IMS");
        logEntry.setImsIdentifier("IMS01");
        logEntry.setImsRegionId(1);
        logEntry.setImsTransname("TRANS1");
        logEntry.setImsAppname("APP1");
        logEntry.setImsPsbname("PSB1");

        String json = logEntry.toJson();

        assertTrue(json.contains("\"ims\""));
        assertTrue(json.contains("\"identifier\":\"IMS01\""));
        assertTrue(json.contains("\"regionId\":1"));
        assertTrue(json.contains("\"transname\":\"TRANS1\""));
        assertTrue(json.contains("\"appname\":\"APP1\""));
        assertTrue(json.contains("\"psbname\":\"PSB1\""));
    }

    @Test
    void testImsInformationNotIncludedWhenRequestTypeIsNotIms() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("CICS");
        logEntry.setImsIdentifier("IMS01");
        logEntry.setImsRegionId(1);
        logEntry.setImsTransname("TRANS1");

        String json = logEntry.toJson();

        assertFalse(json.contains("\"ims\""));
        assertFalse(json.contains("IMS01"));
    }

    @Test
    void testImsInformationNotIncludedWhenRequestTypeIsNull() {
        logEntry.setTimestamp(System.currentTimeMillis());
        // Don't set requestType
        logEntry.setImsIdentifier("IMS01");
        logEntry.setImsRegionId(1);

        String json = logEntry.toJson();

        assertFalse(json.contains("\"ims\""));
    }

    @Test
    void testImsInformationPartial() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestType("IMS");
        logEntry.setImsIdentifier("IMS01");
        logEntry.setImsTransname("TRANS1");
        // Don't set regionId, appname, or psbname

        String json = logEntry.toJson();

        assertTrue(json.contains("\"ims\""));
        assertTrue(json.contains("\"identifier\":\"IMS01\""));
        assertTrue(json.contains("\"transname\":\"TRANS1\""));
        assertFalse(json.contains("\"regionId\""));
        assertFalse(json.contains("\"appname\""));
        assertFalse(json.contains("\"psbname\""));
    }

    @Test
    void testCompleteLogEntryWithCics() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setRequestId(123456789L);
        logEntry.setRequestType("CICS");
        logEntry.setMvsJobname("CICSJOB");
        logEntry.setMvsJobid("JOB00123");
        logEntry.setMvsSysname("SYSA");
        logEntry.setCicsApplid("CICS01");
        logEntry.setCicsTaskNumber(54321);
        logEntry.setCicsTransid("ABCD");
        logEntry.setUser("testuser");
        logEntry.setMappedUser("TESTUSER");
        logEntry.setEndpointHost("api.backend.com");
        logEntry.setEndpointPort(443);
        logEntry.setEndpointMethod("POST");
        logEntry.setEndpointPath("/api/test");
        logEntry.setResponseStatusCode(200);

        String json = logEntry.toJson();

        // Verify all components are present
        assertTrue(json.contains("\"timestamp\":\"2009-02-13T23:31:30.000Z\""));
        assertTrue(json.contains("\"requestId\":123456789"));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"requestType\":\"CICS\""));
        assertFalse(json.contains("\"zos\""));
        assertTrue(json.contains("\"jobname\":\"CICSJOB\""));
        assertTrue(json.contains("\"jobid\":\"JOB00123\""));
        assertTrue(json.contains("\"sysname\":\"SYSA\""));
        assertTrue(json.contains("\"cics\""));
        assertTrue(json.contains("\"applid\":\"CICS01\""));
        assertTrue(json.contains("\"taskNumber\":54321"));
        assertTrue(json.contains("\"transid\":\"ABCD\""));
        assertTrue(json.contains("\"user\":\"testuser\""));
        assertTrue(json.contains("\"mappedUser\":\"TESTUSER\""));
        assertTrue(json.contains("\"host\":\"api.backend.com\""));
        assertTrue(json.contains("\"statusCode\":200"));
    }

    @Test
    void testCompleteLogEntryWithIms() {
        logEntry.setTimestamp(1234567890000L);
        logEntry.setRequestId(987654321L);
        logEntry.setRequestType("IMS");
        logEntry.setMvsJobname("IMSJOB");
        logEntry.setMvsJobid("JOB00456");
        logEntry.setMvsSysname("SYSB");
        logEntry.setImsIdentifier("IMS01");
        logEntry.setImsRegionId(2);
        logEntry.setImsTransname("IMSTRAN");
        logEntry.setImsAppname("IMSAPP");
        logEntry.setImsPsbname("IMSPSB");
        logEntry.setUser("imsuser");
        logEntry.setMappedUser("IMSUSER");
        logEntry.setEndpointHost("ims.backend.com");
        logEntry.setEndpointPort(8080);
        logEntry.setEndpointMethod("GET");
        logEntry.setEndpointPath("/ims/api");
        logEntry.setResponseStatusCode(201);

        String json = logEntry.toJson();

        // Verify all components are present
        assertTrue(json.contains("\"timestamp\":\"2009-02-13T23:31:30.000Z\""));
        assertTrue(json.contains("\"requestId\":987654321"));
        assertTrue(json.contains("\"request\""));
        assertTrue(json.contains("\"requestType\":\"IMS\""));
        assertFalse(json.contains("\"zos\""));
        assertTrue(json.contains("\"jobname\":\"IMSJOB\""));
        assertTrue(json.contains("\"jobid\":\"JOB00456\""));
        assertTrue(json.contains("\"sysname\":\"SYSB\""));
        assertTrue(json.contains("\"ims\""));
        assertTrue(json.contains("\"identifier\":\"IMS01\""));
        assertTrue(json.contains("\"regionId\":2"));
        assertTrue(json.contains("\"transname\":\"IMSTRAN\""));
        assertTrue(json.contains("\"appname\":\"IMSAPP\""));
        assertTrue(json.contains("\"psbname\":\"IMSPSB\""));
        assertTrue(json.contains("\"user\":\"imsuser\""));
        assertTrue(json.contains("\"mappedUser\":\"IMSUSER\""));
        assertTrue(json.contains("\"host\":\"ims.backend.com\""));
        assertTrue(json.contains("\"statusCode\":201"));
    }

    @Test
    void testJsonStructureOrder() {
        logEntry.setTimestamp(System.currentTimeMillis());
        logEntry.setRequestId(123L);
        logEntry.setUser("user1");
        logEntry.setRequestType("CICS");
        logEntry.setMvsJobname("JOB1");
        logEntry.setCicsApplid("CICS01");

        String json = logEntry.toJson();

        // Verify order: timestamp, requestId, user, request (containing requestType, jobname, cics), endpoint, response
        int timestampIndex = json.indexOf("\"timestamp\"");
        int requestIdIndex = json.indexOf("\"requestId\"");
        int userIndex = json.indexOf("\"user\"");
        int requestIndex = json.indexOf("\"request\"");
        int requestTypeIndex = json.indexOf("\"requestType\"");
        int jobnameIndex = json.indexOf("\"jobname\"");
        int cicsIndex = json.indexOf("\"cics\"");

        assertTrue(timestampIndex < requestIdIndex);
        assertTrue(requestIdIndex < userIndex);
        assertTrue(userIndex < requestIndex);
        assertTrue(requestIndex < requestTypeIndex);
        assertTrue(requestTypeIndex < jobnameIndex);
        assertTrue(jobnameIndex < cicsIndex);
    }
}
