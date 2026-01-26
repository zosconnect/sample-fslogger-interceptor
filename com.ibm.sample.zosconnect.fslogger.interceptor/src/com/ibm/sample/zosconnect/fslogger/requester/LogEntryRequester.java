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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

/**
 * LogEntryRequester represents a structured log entry for a z/OS Connect API Requester request/response.
 * 
 * This class captures all relevant data from the API Requester request flow and provides
 * JSON serialization for writing to log files.
 * 
 * @author IBM
 */
public class LogEntryRequester {

    private static final SimpleDateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    static {
        ISO_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    // Timing information
    private long timestamp;
    private Long requestId;

    // Request type and z/OS information
    private String requestType;
    private String mvsJobname;
    private String mvsJobid;
    private String mvsSysname;

    // CICS-specific information
    private String cicsApplid;
    private Integer cicsTaskNumber;
    private String cicsTransid;

    // IMS-specific information
    private String imsIdentifier;
    private Integer imsRegionId;
    private String imsTransname;
    private String imsAppname;
    private String imsPsbname;

    // Endpoint information
    private String endpointHost;
    private Integer endpointPort;
    private String endpointMethod;
    private String endpointPath;
    private String queryString;

    // Request/Response bodies
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Integer requestSize;
    private String user;
    private String mappedUser;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private Integer responseSize;

    // Response information
    private Integer responseStatusCode;

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
     * Sets the request type (e.g., "CICS", "IMS", "BATCH").
     */
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    /**
     * Sets the MVS job name.
     */
    public void setMvsJobname(String mvsJobname) {
        this.mvsJobname = mvsJobname;
    }

    /**
     * Sets the MVS job ID.
     */
    public void setMvsJobid(String mvsJobid) {
        this.mvsJobid = mvsJobid;
    }

    /**
     * Sets the MVS system name.
     */
    public void setMvsSysname(String mvsSysname) {
        this.mvsSysname = mvsSysname;
    }

    /**
     * Sets the CICS application ID.
     */
    public void setCicsApplid(String cicsApplid) {
        this.cicsApplid = cicsApplid;
    }

    /**
     * Sets the CICS task number.
     */
    public void setCicsTaskNumber(Integer cicsTaskNumber) {
        this.cicsTaskNumber = cicsTaskNumber;
    }

    /**
     * Sets the CICS transaction ID.
     */
    public void setCicsTransid(String cicsTransid) {
        this.cicsTransid = cicsTransid;
    }

    /**
     * Sets the IMS identifier.
     */
    public void setImsIdentifier(String imsIdentifier) {
        this.imsIdentifier = imsIdentifier;
    }

    /**
     * Sets the IMS region ID.
     */
    public void setImsRegionId(Integer imsRegionId) {
        this.imsRegionId = imsRegionId;
    }

    /**
     * Sets the IMS transaction name.
     */
    public void setImsTransname(String imsTransname) {
        this.imsTransname = imsTransname;
    }

    /**
     * Sets the IMS application name.
     */
    public void setImsAppname(String imsAppname) {
        this.imsAppname = imsAppname;
    }

    /**
     * Sets the IMS PSB name.
     */
    public void setImsPsbname(String imsPsbname) {
        this.imsPsbname = imsPsbname;
    }

    /**
     * Sets the endpoint host.
     */
    public void setEndpointHost(String endpointHost) {
        this.endpointHost = endpointHost;
    }

    /**
     * Sets the endpoint port.
     */
    public void setEndpointPort(Integer endpointPort) {
        this.endpointPort = endpointPort;
    }

    /**
     * Sets the endpoint HTTP method.
     */
    public void setEndpointMethod(String endpointMethod) {
        this.endpointMethod = endpointMethod;
    }

    /**
     * Sets the endpoint path.
     */
    public void setEndpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
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
     * Sets the response status code returned to the calling application.
     */
    public void setResponseStatusCode(Integer responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }

    /**
     * Gets the response status code.
     */
    public Integer getResponseStatusCode() {
        return responseStatusCode;
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

        // Request object
        json.append(",\"request\":{");
        boolean requestFirst = true;
        
        // Request Type 
        if (requestType != null) {
            json.append("\"requestType\":").append(escapeJson(requestType));
            requestFirst = false;
        }

        // z/OS Information
        if (mvsJobname != null) {
            if (!requestFirst) json.append(",");
            json.append("\"jobname\":").append(escapeJson(mvsJobname));
            requestFirst = false;
        }
        
        if (mvsJobid != null) {
            if (!requestFirst) json.append(",");
            json.append("\"jobid\":").append(escapeJson(mvsJobid));
            requestFirst = false;
        }
        
        if (mvsSysname != null) {
            if (!requestFirst) json.append(",");
            json.append("\"sysname\":").append(escapeJson(mvsSysname));
            requestFirst = false;
        }

        // CICS Information (only if requestType is CICS)
        if ("CICS".equals(requestType) && (cicsApplid != null || cicsTaskNumber != null || cicsTransid != null)) {
            json.append(",\"cics\":{");
            boolean cicsFirst = true;
            
            if (cicsApplid != null) {
                json.append("\"applid\":").append(escapeJson(cicsApplid));
                cicsFirst = false;
            }
            
            if (cicsTaskNumber != null) {
                if (!cicsFirst) json.append(",");
                json.append("\"taskNumber\":").append(cicsTaskNumber);
                cicsFirst = false;
            }
            
            if (cicsTransid != null) {
                if (!cicsFirst) json.append(",");
                json.append("\"transid\":").append(escapeJson(cicsTransid));
            }
            
            json.append("}");
        }

        // IMS Information (only if requestType is IMS)
        if ("IMS".equals(requestType) && (imsIdentifier != null || imsRegionId != null ||
            imsTransname != null || imsAppname != null || imsPsbname != null)) {
            json.append(",\"ims\":{");
            boolean imsFirst = true;
            
            if (imsIdentifier != null) {
                json.append("\"identifier\":").append(escapeJson(imsIdentifier));
                imsFirst = false;
            }
            
            if (imsRegionId != null) {
                if (!imsFirst) json.append(",");
                json.append("\"regionId\":").append(imsRegionId);
                imsFirst = false;
            }
            
            if (imsTransname != null) {
                if (!imsFirst) json.append(",");
                json.append("\"transname\":").append(escapeJson(imsTransname));
                imsFirst = false;
            }
            
            if (imsAppname != null) {
                if (!imsFirst) json.append(",");
                json.append("\"appname\":").append(escapeJson(imsAppname));
                imsFirst = false;
            }
            
            if (imsPsbname != null) {
                if (!imsFirst) json.append(",");
                json.append("\"psbname\":").append(escapeJson(imsPsbname));
            }
            
            json.append("}");
        }
        
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            if (!requestFirst) json.append(",");
            json.append("\"headers\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                if (!first) json.append(",");
                json.append(escapeJson(entry.getKey())).append(":").append(escapeJson(entry.getValue()));
                first = false;
            }
            json.append("}");
            requestFirst = false;
        }
        
        if (requestBody != null) {
            if (!requestFirst) json.append(",");
            json.append("\"body\":").append(escapeJson(requestBody));
            requestFirst = false;
        }
        if (requestSize != null) {
            if (!requestFirst) json.append(",");
            json.append("\"size\":").append(requestSize);
        }
        json.append("}");

        // Endpoint information
        if (endpointHost != null || endpointPort != null || endpointMethod != null ||
            endpointPath != null || queryString != null) {
            json.append(",\"endpoint\":{");
            boolean first = true;
            
            if (endpointHost != null) {
                json.append("\"host\":").append(escapeJson(endpointHost));
                first = false;
            }
            
            if (endpointPort != null) {
                if (!first) json.append(",");
                json.append("\"port\":").append(endpointPort);
                first = false;
            }
            
            if (endpointMethod != null) {
                if (!first) json.append(",");
                json.append("\"method\":").append(escapeJson(endpointMethod));
                first = false;
            }
            
            if (endpointPath != null) {
                if (!first) json.append(",");
                json.append("\"path\":").append(escapeJson(endpointPath));
                first = false;
            }
            
            if (queryString != null) {
                if (!first) json.append(",");
                json.append("\"queryString\":").append(escapeJson(queryString));
                first = false;
            }
                       
            json.append("}");
        }

        // Response object
        json.append(",\"response\":{");
        boolean responseFirst = true;
             
        if (responseStatusCode != null) {
            if (!responseFirst) json.append(",");
            json.append("\"statusCode\":").append(responseStatusCode);
            responseFirst = false;
        }
        
        if (responseHeaders != null && !responseHeaders.isEmpty()) {
            if (!responseFirst) json.append(",");
            json.append("\"headers\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                if (!first) json.append(",");
                json.append(escapeJson(entry.getKey())).append(":").append(escapeJson(entry.getValue()));
                first = false;
            }
            json.append("}");
            responseFirst = false;
        }
        
        if (responseBody != null) {
            if (!responseFirst) json.append(",");
            json.append("\"body\":").append(escapeJson(responseBody));
            responseFirst = false;
        }
        
        if (responseSize != null) {
            if (!responseFirst) json.append(",");
            json.append("\"size\":").append(responseSize);
        }
        
        json.append("}");
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
