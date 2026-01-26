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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * LogEntryProvider represents a structured log entry for a z/OS Connect API Provider request/response.
 *
 * This class captures all relevant data from the API Provider request flow and provides
 * JSON serialization for writing to log files.
 *
 * @author IBM
 */
public class LogEntryProvider {

    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    static {
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Timing information
    private long timestamp;
    private Long requestId;

    // Request information
    private String requestUri;
    private String requestMethod;
    private String queryString;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Integer requestSize;
    private String user;
    private String mappedUser;

    // System of Record information
    private String sorIdentifier;
    private String sorResource;
    private String sorReference;

    // Response information
    private Integer responseCode;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private Integer responseSize;
    private boolean timedOut;

    /**
     * Sets the timestamp when the log entry was created.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the request ID.
     */
    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    /**
     * Sets the request URI.
     */
    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    /**
     * Sets the request method (GET, POST, etc.).
     */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * Sets the query string.
     */
    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * Sets the request headers.
     */
    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    /**
     * Sets the request body.
     */
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Sets the request size in bytes.
     */
    public void setRequestSize(Integer requestSize) {
        this.requestSize = requestSize;
    }

    /**
     * Sets the user name.
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the mapped user name.
     */
    public void setMappedUser(String mappedUser) {
        this.mappedUser = mappedUser;
    }

    /**
     * Sets the System of Record identifier.
     */
    public void setSorIdentifier(String sorIdentifier) {
        this.sorIdentifier = sorIdentifier;
    }

    /**
     * Sets the System of Record resource.
     */
    public void setSorResource(String sorResource) {
        this.sorResource = sorResource;
    }

    /**
     * Sets the System of Record reference.
     */
    public void setSorReference(String sorReference) {
        this.sorReference = sorReference;
    }

    /**
     * Sets the HTTP response code.
     */
    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Sets the response headers.
     */
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Sets the response body.
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Sets the response size in bytes.
     */
    public void setResponseSize(Integer responseSize) {
        this.responseSize = responseSize;
    }

    /**
     * Sets whether the request timed out.
     */
    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    /**
     * Gets the HTTP response code.
     */
    public Integer getResponseCode() {
        return responseCode;
    }

    /**
     * Converts the log entry to a JSON string.
     * 
     * This method manually constructs JSON to avoid dependencies on external libraries.
     * For production use, consider using a proper JSON library like Jackson or Gson.
     * 
     * @return JSON representation of the log entry
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Timestamp
        json.append("\"timestamp\":\"").append(ISO_DATE_FORMAT.format(new Date(timestamp))).append("\"");

        // Request ID
        if (requestId != null) {
            json.append(",\"requestId\":").append(requestId);
        }

        // User information
        if (user != null) {
            json.append(",\"user\":").append(escapeJson(user));
        }
        if (mappedUser != null) {
            json.append(",\"mappedUser\":").append(escapeJson(mappedUser));
        }

        // Request information
        json.append(",\"request\":{");
        if (requestUri != null) {
            json.append("\"uri\":").append(escapeJson(requestUri));
        }
        if (requestMethod != null) {
            json.append(",\"method\":").append(escapeJson(requestMethod));
        }
        if (queryString != null) {
            json.append(",\"queryString\":").append(escapeJson(queryString));
        }
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            json.append(",\"headers\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                if (!first) json.append(",");
                json.append(escapeJson(entry.getKey())).append(":").append(escapeJson(entry.getValue()));
                first = false;
            }
            json.append("}");
        }
        if (requestBody != null) {
            json.append(",\"body\":").append(escapeJson(requestBody));
        }
        if (requestSize != null) {
            json.append(",\"size\":").append(requestSize);
        }
        json.append("}");

        // System of Record information
        if (sorIdentifier != null || sorResource != null || sorReference != null) {
            json.append(",\"sor\":{");
            boolean first = true;
            if (sorIdentifier != null) {
                json.append("\"identifier\":").append(escapeJson(sorIdentifier));
                first = false;
            }
            if (sorResource != null) {
                if (!first) json.append(",");
                json.append("\"resource\":").append(escapeJson(sorResource));
                first = false;
            }
            if (sorReference != null) {
                if (!first) json.append(",");
                json.append("\"reference\":").append(escapeJson(sorReference));
                first = false;
            }
            json.append("}");
        }

        // Response information
        json.append(",\"response\":{");
        if (responseCode != null) {
            json.append("\"statusCode\":").append(responseCode);
        }
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            json.append(",\"headers\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                if (!first) json.append(",");
                json.append(escapeJson(entry.getKey())).append(":").append(escapeJson(entry.getValue()));
                first = false;
            }
            json.append("}");
        }
        if (responseBody != null) {
            json.append(",\"body\":").append(escapeJson(responseBody));
        }
        if (responseSize != null) {
            json.append(",\"size\":").append(responseSize);
        }
        json.append("}");

        // Timed out flag
        if (timedOut) {
            json.append(",\"timedOut\":true");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Escapes a string for JSON output.
     * 
     * @param value The string to escape
     * @return The escaped JSON string with quotes
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        escaped.append("\"");
        return escaped.toString();
    }

    @Override
    public String toString() {
        return toJson();
    }
}
