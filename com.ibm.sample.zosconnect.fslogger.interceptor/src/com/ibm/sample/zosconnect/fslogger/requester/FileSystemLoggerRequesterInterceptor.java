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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;

import com.ibm.zosconnect.spi.Data;
import com.ibm.zosconnect.spi.DataRequester;
import com.ibm.zosconnect.spi.EndpointInterceptor;
import com.ibm.zosconnect.spi.HttpZosConnectRequest;
import com.ibm.zosconnect.spi.InterceptorException;
import com.ibm.sample.zosconnect.fslogger.common.RollingFileHandler;

/**
 * FileSystemLoggerRequesterInterceptor captures z/OS Connect API Requester request and response data
 * and writes it to rolling log files on the file system.
 * 
 * This interceptor implements EndpointInterceptor to capture data at the API requester
 * interception points in the z/OS Connect API Requester request flow.
 * 
 * API Requester Flow:
 * 1. preInvokeRequester - After initial checks, before data mapping
 * 2. preEndpointInvoke - Just before calling the endpoint
 * 3. postEndpointInvoke - After endpoint returns
 * 4. postInvokeRequester - Before returning to calling application
 * 
 * Configuration properties:
 * - id: The unique ID of this Interceptor
 * - logDirectory: Directory path where log files will be written
 * - maxFileSize: Maximum size of each log file in bytes (default: 10MB)
 * - maxFileCount: Maximum number of log files to retain (default: 10)
 * - requestHeaders: Comma-separated list of request header names to capture
 * - responseHeaders: Comma-separated list of response header names to capture
 * - includeBody: Include request and response bodies (default: false)
 * - sequence: Interceptor execution order
 * 
 * @author IBM
 */
public class FileSystemLoggerRequesterInterceptor implements EndpointInterceptor {

    /**
     * Request State Map key for storing log entry data across interception points
     */
    private static final String LOG_ENTRY_KEY = "FS_LOGGER_REQUESTER_ENTRY";

    /** 
     * The id specified in the configuration for this Interceptor
     */
    private String id;

    /**
     * The registered sequence number of this Interceptor
     */
    private int sequence;

    /**
     * Directory path where log files will be written
     */
    private String logDirectory;

    /**
     * Maximum size of each log file in bytes
     */
    private long maxFileSize = 10485760L; // 10MB default

    /**
     * Maximum number of log files to retain
     */
    private int maxFileCount = 10;

    /**
     * Comma-separated list of request header names to capture
     */
    private String requestHeaders = "";

    /**
     * Comma-separated list of response header names to capture
     */
    private String responseHeaders = "";

    /**
     * Parsed set of request header names (case-insensitive)
     */
    private Set<String> requestHeaderNames = new HashSet<>();

    /**
     * Parsed set of response header names (case-insensitive)
     */
    private Set<String> responseHeaderNames = new HashSet<>();

    /**
     * Whether to include request and response bodies
     */
    private boolean includeBody = false;

    /**
     * Rolling file handler for managing log files
     */
    private RollingFileHandler fileHandler;

    /**
     * Activates the Interceptor.
     * 
     * Retrieves configuration from server.xml and initializes the file handler.
     * 
     * @param context OSGi component context
     * @param properties Configuration properties from server.xml
     */
    protected void activate(ComponentContext context, Map<String, Object> properties) {

        // Get unique id
        if (properties != null) {
            id = (String) properties.get("id");
            System.out.println(getName() + " activated");
        } else {
            System.err.println(getName() + " ERROR: Properties are not available");
            return;
        }

        // Get sequence number
        if (properties.containsKey(CFG_AD_SEQUENCE_ALIAS)) {
            sequence = (Integer) properties.get(CFG_AD_SEQUENCE_ALIAS);
        }

        // Get log directory (required)
        if (properties.containsKey("logDirectory")) {
            logDirectory = (String) properties.get("logDirectory");
        } else {
            System.err.println(getName() + " ERROR: logDirectory configuration is required");
            return;
        }

        // Get max file size (optional)
        if (properties.containsKey("maxFileSize")) {
            maxFileSize = (Long) properties.get("maxFileSize");
        }

        // Get max file count (optional)
        if (properties.containsKey("maxFileCount")) {
            maxFileCount = (Integer) properties.get("maxFileCount");
        }

        // Get request headers list (optional)
        if (properties.containsKey("requestHeaders")) {
            requestHeaders = (String) properties.get("requestHeaders");
            requestHeaderNames = parseHeaderNames(requestHeaders);
        }

        // Get response headers list (optional)
        if (properties.containsKey("responseHeaders")) {
            responseHeaders = (String) properties.get("responseHeaders");
            responseHeaderNames = parseHeaderNames(responseHeaders);
        }

        // Get include body flag (optional)
        if (properties.containsKey("includeBody")) {
            includeBody = (Boolean) properties.get("includeBody");
        }

        // Initialize file handler
        try {
            fileHandler = new RollingFileHandler(id, logDirectory, maxFileSize, maxFileCount);
            System.out.println(getName() + " initialized with logDirectory=" + logDirectory +
                             ", maxFileSize=" + maxFileSize + ", maxFileCount=" + maxFileCount +
                             ", requestHeaders=" + requestHeaders + ", responseHeaders=" + responseHeaders +
                             ", includeBody=" + includeBody);
        } catch (IOException e) {
            System.err.println(getName() + " ERROR: Failed to initialize file handler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deactivates the Interceptor.
     * 
     * Closes the file handler and releases resources.
     * 
     * @param context OSGi component context
     */
    protected void deactivate(ComponentContext context) {
        System.out.println(getName() + " deactivated");
        if (fileHandler != null) {
            try {
                fileHandler.close();
            } catch (IOException e) {
                System.err.println(getName() + " ERROR: Failed to close file handler: " + e.getMessage());
            }
        }
    }

    /**
     * Called when the Interceptor's configuration is modified.
     * 
     * @param properties Updated configuration properties
     */
    protected void modified(Map<String, Object> properties) {
        System.out.println(getName() + " modified");

        // Update sequence
        if (properties.containsKey(CFG_AD_SEQUENCE_ALIAS)) {
            sequence = (Integer) properties.get(CFG_AD_SEQUENCE_ALIAS);
        }

        // Reinitialize if configuration changed
        boolean needsReinit = false;

        if (properties.containsKey("logDirectory")) {
            String newLogDirectory = (String) properties.get("logDirectory");
            if (!newLogDirectory.equals(logDirectory)) {
                logDirectory = newLogDirectory;
                needsReinit = true;
            }
        }

        if (properties.containsKey("maxFileSize")) {
            long newMaxFileSize = (Long) properties.get("maxFileSize");
            if (newMaxFileSize != maxFileSize) {
                maxFileSize = newMaxFileSize;
                needsReinit = true;
            }
        }

        if (properties.containsKey("maxFileCount")) {
            int newMaxFileCount = (Integer) properties.get("maxFileCount");
            if (newMaxFileCount != maxFileCount) {
                maxFileCount = newMaxFileCount;
                needsReinit = true;
            }
        }

        // Update request headers list
        if (properties.containsKey("requestHeaders")) {
            requestHeaders = (String) properties.get("requestHeaders");
            requestHeaderNames = parseHeaderNames(requestHeaders);
        }

        // Update response headers list
        if (properties.containsKey("responseHeaders")) {
            responseHeaders = (String) properties.get("responseHeaders");
            responseHeaderNames = parseHeaderNames(responseHeaders);
        }

        // Update include body flag
        if (properties.containsKey("includeBody")) {
            includeBody = (Boolean) properties.get("includeBody");
        }

        if (needsReinit && fileHandler != null) {
            try {
                fileHandler.close();
                fileHandler = new RollingFileHandler(id, logDirectory, maxFileSize, maxFileCount);
                System.out.println(getName() + " reinitialized with new configuration");
            } catch (IOException e) {
                System.err.println(getName() + " ERROR: Failed to reinitialize file handler: " + e.getMessage());
            }
        }
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @Override
    public String getName() {
        return "FileSystemLoggerRequesterInterceptor" + id;
    }

    /**
     * Called after initial request checks, before data mapping.
     * 
     * Captures API requester name, version, and initial timing.
     */
    @Override
    public void preInvokeRequester(Map<Object, Object> requestStateMap, DataRequester data)
            throws InterceptorException {

        if (fileHandler == null) {
            return; // Not properly initialized
        }

        try {
            // Create log entry
            LogEntryRequester logEntry = new LogEntryRequester();

            // Capture timestamp
            logEntry.setTimestamp(System.currentTimeMillis());

            // Capture request ID
            if (data.getData(DataRequester.REQUEST_ID) != null) {
                logEntry.setRequestId((Long) data.getData(DataRequester.REQUEST_ID));
            }

            // Capture request type
            if (data.getData(DataRequester.REQUEST_APPLICATION_TYPE) != null) {
                logEntry.setRequestType((String) data.getData(DataRequester.REQUEST_APPLICATION_TYPE));
            }

            // Capture z/OS information (always)
            if (data.getData(DataRequester.MVS_JOBNAME) != null) {
                logEntry.setMvsJobname((String) data.getData(DataRequester.MVS_JOBNAME));
            }

            if (data.getData(DataRequester.MVS_JOBID) != null) {
                logEntry.setMvsJobid((String) data.getData(DataRequester.MVS_JOBID));
            }

            if (data.getData(DataRequester.MVS_SYSNAME) != null) {
                logEntry.setMvsSysname((String) data.getData(DataRequester.MVS_SYSNAME));
            }

            // Capture CICS information (only if requestType is CICS)
            String requestType = (String) data.getData(DataRequester.REQUEST_APPLICATION_TYPE);
            if ("CICS".equals(requestType)) {
                if (data.getData(DataRequester.CICS_APPLID) != null) {
                    logEntry.setCicsApplid((String) data.getData(DataRequester.CICS_APPLID));
                }

                if (data.getData(DataRequester.CICS_TASK_NUMBER) != null) {
                    logEntry.setCicsTaskNumber((Integer) data.getData(DataRequester.CICS_TASK_NUMBER));
                }

                if (data.getData(DataRequester.CICS_TRANSID) != null) {
                    logEntry.setCicsTransid((String) data.getData(DataRequester.CICS_TRANSID));
                }
            }

            // Capture IMS information (only if requestType is IMS)
            if ("IMS".equals(requestType)) {
                if (data.getData(DataRequester.IMS_IDENTIFIER) != null) {
                    logEntry.setImsIdentifier((String) data.getData(DataRequester.IMS_IDENTIFIER));
                }

                if (data.getData(DataRequester.IMS_REGION_ID) != null) {
                    logEntry.setImsRegionId((Integer) data.getData(DataRequester.IMS_REGION_ID));
                }

                if (data.getData(DataRequester.IMS_TRANSNAME) != null) {
                    logEntry.setImsTransname((String) data.getData(DataRequester.IMS_TRANSNAME));
                }

                if (data.getData(DataRequester.IMS_APPNAME) != null) {
                    logEntry.setImsAppname((String) data.getData(DataRequester.IMS_APPNAME));
                }

                if (data.getData(DataRequester.IMS_PSBNAME) != null) {
                    logEntry.setImsPsbname((String) data.getData(DataRequester.IMS_PSBNAME));
                }
            }

            // Capture user information
            if (data.getData(DataRequester.USER_NAME) != null) {
                logEntry.setUser((String) data.getData(DataRequester.USER_NAME));
            }

            if (data.getData(DataRequester.USER_NAME_MAPPED) != null) {
                logEntry.setMappedUser((String) data.getData(DataRequester.USER_NAME_MAPPED));
            }

            // Capture request headers if configured
            if (!requestHeaderNames.isEmpty()) {
                Map<String, String> capturedHeaders = new HashMap<>();
                Enumeration<String> headerNames = data.getRequestHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    // Case-insensitive comparison
                    if (requestHeaderNames.contains(headerName.toLowerCase())) {
                        String headerValue = data.getRequestHeader(headerName);
                        capturedHeaders.put(headerName, headerValue);
                    }
                }
                if (!capturedHeaders.isEmpty()) {
                    logEntry.setRequestHeaders(capturedHeaders);
                }
            }

            // Capture request body if enabled
            if (includeBody && data.getInputPayload() != null) {
                String requestBody = data.getInputPayload();
                logEntry.setRequestBody(requestBody);
                logEntry.setRequestSize(requestBody.getBytes("UTF-8").length);
            } else if (data.getInputPayload() != null) {
                // Even if not including body, capture the size
                logEntry.setRequestSize(data.getInputPayload().getBytes("UTF-8").length);
            }

            // Store log entry in request state map for later use
            requestStateMap.put(LOG_ENTRY_KEY, logEntry);

        } catch (Exception e) {
            // Don't throw InterceptorException - we don't want to fail the request
            System.err.println(getName() + " ERROR in preInvokeRequester: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called just before calling the endpoint.
     * 
     * Captures endpoint host, port, method, and path information.
     */
    @Override
    public void preEndpointInvoke(Map<Object, Object> requestStateMap, DataRequester data) {

        if (fileHandler == null) {
            return;
        }

        try {
            LogEntryRequester logEntry = (LogEntryRequester) requestStateMap.get(LOG_ENTRY_KEY);
            if (logEntry == null) {
                return;
            }

            // Capture endpoint information
            if (data.getData(DataRequester.ENDPOINT_HOST) != null) {
                logEntry.setEndpointHost((String) data.getData(DataRequester.ENDPOINT_HOST));
            }
            
            if (data.getData(DataRequester.ENDPOINT_PORT) != null) {
                logEntry.setEndpointPort((Integer) data.getData(DataRequester.ENDPOINT_PORT));
            }
            
            if (data.getData(DataRequester.ENDPOINT_FULL_PATH) != null) {
                logEntry.setEndpointPath((String) data.getData(DataRequester.ENDPOINT_FULL_PATH));
            }

            // Capture HTTP method
            if (data.getData(DataRequester.ENDPOINT_METHOD) != null) {
                logEntry.setEndpointMethod((String) data.getData(DataRequester.ENDPOINT_METHOD));
            }

            // Capture query string
            if (data.getData(DataRequester.ENDPOINT_QUERY_STRING) != null) {
                logEntry.setQueryString((String) data.getData(DataRequester.ENDPOINT_QUERY_STRING));
            }

        } catch (Exception e) {
            System.err.println(getName() + " ERROR in preEndpointInvoke: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called after endpoint returns.
     * 
     * Captures HTTP response code from the endpoint.
     */
    @Override
    public void postEndpointInvoke(Map<Object, Object> requestStateMap, DataRequester data) {

        if (fileHandler == null) {
            return;
        }

        try {
            LogEntryRequester logEntry = (LogEntryRequester) requestStateMap.get(LOG_ENTRY_KEY);
            if (logEntry == null) {
                return;
            }

            // Capture endpoint response code
            if (data.getData(DataRequester.HTTP_RESPONSE_CODE) != null) {
                logEntry.setResponseStatusCode((Integer) data.getData(DataRequester.HTTP_RESPONSE_CODE));
            }

        } catch (Exception e) {
            System.err.println(getName() + " ERROR in postEndpointInvoke: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Called before returning to calling application.
     * 
     * Captures final request status and writes the complete log entry to file.
     */
    @Override
    public void postInvokeRequester(Map<Object, Object> requestStateMap, DataRequester data)
            throws InterceptorException {

        if (fileHandler == null) {
            return;
        }

        try {
            LogEntryRequester logEntry = (LogEntryRequester) requestStateMap.get(LOG_ENTRY_KEY);
            if (logEntry == null) {
                return;
            }

            // Capture response headers if configured
            if (!responseHeaderNames.isEmpty()) {
                Map<String, String> capturedHeaders = new HashMap<>();
                Enumeration<String> headerNames = data.getResponseHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    // Case-insensitive comparison
                    if (responseHeaderNames.contains(headerName.toLowerCase())) {
                        String headerValue = data.getResponseHeader(headerName);
                        capturedHeaders.put(headerName, headerValue);
                    }
                }
                if (!capturedHeaders.isEmpty()) {
                    logEntry.setResponseHeaders(capturedHeaders);
                }
            }

            // Capture response body if enabled
            if (includeBody && data.getOutputPayload() != null) {
                String responseBody = data.getOutputPayload();
                logEntry.setResponseBody(responseBody);
                logEntry.setResponseSize(responseBody.getBytes("UTF-8").length);
            } else if (data.getOutputPayload() != null) {
                // Even if not including body, capture the size
                logEntry.setResponseSize(data.getOutputPayload().getBytes("UTF-8").length);
            }

            // Write log entry to file
            fileHandler.writeLog(logEntry.toJson());

        } catch (Exception e) {
            System.err.println(getName() + " ERROR in postInvokeRequester: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method is not called for API requester requests.
     * It is called for API provider and administration requests.
     */
    @Override
    public void preInvoke(Map<Object, Object> requestStateMap, HttpZosConnectRequest request, Data data)
            throws InterceptorException {
        // Not used for API requester
    }

    /**
     * This method is not called for API requester requests.
     * It is called for API provider and administration requests.
     */
    @Override
    public void postInvoke(Map<Object, Object> requestStateMap, HttpZosConnectRequest request, Data data)
            throws InterceptorException {
        // Not used for API requester
    }

    /**
     * Parses a comma-separated list of header names into a Set.
     * Header names are converted to lowercase for case-insensitive matching.
     *
     * @param headerList Comma-separated list of header names
     * @return Set of lowercase header names
     */
    private Set<String> parseHeaderNames(String headerList) {
        Set<String> names = new HashSet<>();
        if (headerList != null && !headerList.trim().isEmpty()) {
            String[] parts = headerList.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed.toLowerCase());
                }
            }
        }
        return names;
    }
}
