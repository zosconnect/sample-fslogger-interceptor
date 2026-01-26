# Test Suite Documentation

## Overview

This directory contains comprehensive unit tests for the File System Logger Interceptor components. The test suite uses JUnit 5 and Mockito to ensure code quality and reliability.

## Test Structure

```
test/
├── com/ibm/sample/zosconnect/fslogger/
│   ├── common/
│   │   └── RollingFileHandlerTest.java
│   ├── provider/
│   │   ├── LogEntryProviderTest.java
│   │   └── FileSystemLoggerProviderInterceptorTest.java
│   └── requester/
│       ├── LogEntryRequesterTest.java
│       └── FileSystemLoggerRequesterInterceptorTest.java
└── README.md
```

## Test Coverage

### RollingFileHandlerTest (325 lines, 14 tests)

Tests the core file rotation and management functionality:

- **File Creation**: Validates log file creation with proper naming conventions
- **Directory Validation**: Tests error handling for invalid directories
- **Write Operations**: Tests single and multiple log entry writes
- **File Rotation**: Validates automatic rotation when size limits are exceeded
- **File Cleanup**: Tests deletion of old files when max count is reached
- **Thread Safety**: Concurrent write operations from multiple threads
- **Resource Management**: Proper cleanup and resource release

**Key Test Cases:**
- `testConstructorCreatesLogFile()` - Verifies initial file creation
- `testFileRotationWhenMaxSizeExceeded()` - Tests automatic rotation
- `testMaxFileCountEnforcement()` - Validates file count limits
- `testThreadSafety()` - Concurrent access testing
- `testCloseReleasesResources()` - Resource cleanup verification

### LogEntryProviderTest (370 lines, 27 tests)

Tests JSON serialization and data capture for API Provider requests:

- **JSON Serialization**: Validates correct JSON output format
- **Timestamp Formatting**: ISO 8601 UTC timestamp format
- **Request Data**: URI, method, headers, body, user information
- **SoR Information**: System of Record identifier, resource, reference
- **Response Data**: Status code, headers, body, timeout flag
- **Character Escaping**: Special characters, control characters, Unicode
- **Null Handling**: Graceful handling of missing optional fields

**Key Test Cases:**
- `testBasicJsonSerialization()` - Core JSON structure
- `testRequestHeaders()` - Header capture and serialization
- `testSorInformation()` - System of Record data
- `testJsonEscaping()` - Special character handling
- `testCompleteLogEntry()` - Full request/response cycle

### LogEntryRequesterTest (793 lines, 46 tests)

Tests JSON serialization and data capture for API Requester requests:

- **JSON Serialization**: Validates correct JSON output format
- **Timestamp Formatting**: ISO 8601 UTC timestamp format
- **Request Type**: CICS, IMS, and other request types
- **z/OS Information**: jobname, jobid, sysname (direct children of request object)
- **CICS Information**: applid, taskNumber, transid (only when requestType is CICS)
- **IMS Information**: identifier, regionId, transname, appname, psbname (only when requestType is IMS)
- **User Information**: user and mappedUser fields
- **Endpoint Information**: Host, port, method, path, queryString
- **Request/Response Data**: Headers, body, size tracking
- **Status Codes**: HTTP response code capture
- **Character Handling**: Escaping, Unicode, control characters
- **Null Handling**: Optional field management
- **JSON Structure Order**: Validates correct field ordering in output

**Key Test Cases:**
- `testBasicJsonSerialization()` - Core JSON structure
- `testEndpointInformation()` - Endpoint data capture
- `testZosInformation()` - z/OS system information (jobname, jobid, sysname without zos wrapper)
- `testCicsInformationWhenRequestTypeIsCics()` - CICS-specific data capture
- `testImsInformationWhenRequestTypeIsIms()` - IMS-specific data capture
- `testRequestHeaders()` - Header capture
- `testCompleteLogEntryWithCics()` - Full CICS request/response cycle
- `testCompleteLogEntryWithIms()` - Full IMS request/response cycle
- `testJsonStructureOrder()` - Validates field ordering

### FileSystemLoggerProviderInterceptorTest (441 lines, 18 tests)

Tests the API Provider interceptor lifecycle and behavior:

- **Lifecycle Management**: Activation, deactivation, modification
- **Configuration**: Property handling, validation, defaults
- **Interception Points**: preInvoke, preSorInvoke, postSorInvoke, postInvoke
- **Data Capture**: Request/response data at each interception point
- **Header Filtering**: Case-insensitive header matching
- **File Writing**: Log file creation and content verification
- **Error Handling**: Graceful handling of missing configuration

**Key Test Cases:**
- `testActivateWithValidConfiguration()` - Initialization
- `testPreInvokeCreatesLogEntry()` - Log entry creation
- `testPreInvokeCapturesRequestHeaders()` - Header filtering
- `testPostInvokeWritesLogFile()` - File output verification
- `testCaseInsensitiveHeaderMatching()` - Header matching logic

### FileSystemLoggerRequesterInterceptorTest (868 lines, 29 tests)

Tests the API Requester interceptor lifecycle and behavior:

- **Lifecycle Management**: Activation, deactivation, modification
- **Configuration**: Property handling, validation
- **Interception Points**: preInvokeRequester, preEndpointInvoke, postEndpointInvoke, postInvokeRequester
- **Request Type Capture**: CICS, IMS, and other request types
- **z/OS Information**: jobname, jobid, sysname capture (without zos wrapper)
- **CICS Information**: applid, taskNumber, transid (conditional on request type)
- **IMS Information**: identifier, regionId, transname, appname, psbname (conditional on request type)
- **User Information**: user and mappedUser capture
- **Endpoint Data**: Host, port, method, path, queryString capture
- **Request/Response**: Header and body capture with size tracking
- **Header Filtering**: Case-insensitive header matching
- **File Writing**: Log file creation and content verification
- **Complete Flow**: End-to-end request processing with all fields

**Key Test Cases:**
- `testActivateWithValidConfiguration()` - Initialization
- `testPreInvokeRequesterCreatesLogEntry()` - Log entry creation
- `testRequestTypeCapture()` - Request type identification
- `testZosInformationCapture()` - z/OS system information (verifies no zos wrapper)
- `testCicsInformationCapture()` - CICS-specific data capture
- `testImsInformationCapture()` - IMS-specific data capture
- `testUserAndMappedUserCapture()` - User information capture
- `testPreEndpointInvokeCapturesEndpointData()` - Endpoint information
- `testQueryStringCapture()` - Query string handling
- `testCompleteRequestFlow()` - Full request lifecycle
- `testCompleteRequestWithAllNewFields()` - Comprehensive field verification
- `testPostInvokeRequesterWritesLogFile()` - File output

## Running Tests

### Run All Tests

```bash
gradle test
```

### Run Specific Test Class

```bash
gradle test --tests RollingFileHandlerTest
gradle test --tests LogEntryProviderTest
gradle test --tests FileSystemLoggerProviderInterceptorTest
```

### Run Specific Test Method

```bash
gradle test --tests RollingFileHandlerTest.testFileRotationWhenMaxSizeExceeded
```

### Run Tests with Detailed Output

```bash
gradle test --info
```

### Generate Test Report

```bash
gradle test
# Report available at: build/reports/tests/test/index.html
```

## Test Configuration

Tests are configured in `build.gradle`:

```gradle
test {
    useJUnitPlatform()
    
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
        showStandardStreams = false
    }
    
    doFirst {
        def testLogDir = file("${buildDir}/test-logs")
        testLogDir.mkdirs()
        systemProperty 'test.log.dir', testLogDir.absolutePath
    }
}
```

## Dependencies

- **JUnit 5** (5.9.3): Modern testing framework
- **Mockito** (5.3.1): Mocking framework for dependencies
- **JUnit Jupiter Params**: Parameterized test support

## Test Best Practices

1. **Isolation**: Each test is independent and can run in any order
2. **Cleanup**: `@AfterEach` ensures proper resource cleanup
3. **Temp Directories**: `@TempDir` provides isolated file system for each test
4. **Mocking**: External dependencies are mocked to focus on unit behavior
5. **Assertions**: Clear, descriptive assertions with meaningful messages
6. **Coverage**: Tests cover happy paths, edge cases, and error conditions

## Continuous Integration

Tests are automatically run during the build process:

```bash
gradle build
```

The build will fail if any tests fail, ensuring code quality.

## Test Metrics

- **Total Test Classes**: 5
- **Total Test Methods**: 130
- **Lines of Test Code**: ~2,700
- **Code Coverage**: Comprehensive coverage of all public methods and critical paths

## Troubleshooting

### Tests Fail Due to Missing z/OS Connect SPI

If you see compilation errors related to `com.ibm.zosconnect.spi`, you need to install the z/OS Connect SPI JAR to your local Maven repository:

```bash
mvn install:install-file \
  -Dfile=path/to/com.ibm.zosconnect.spi.jar \
  -DgroupId=com.ibm.zosconnect \
  -DartifactId=com.ibm.zosconnect.spi \
  -Dversion=2.0.0.0 \
  -Dpackaging=jar
```

### Tests Fail Due to File Permissions

Ensure the test process has write permissions to the build directory:

```bash
chmod -R 755 build/
```

### Tests Timeout

If tests timeout, increase the timeout in `build.gradle`:

```gradle
test {
    timeout = Duration.ofMinutes(10)
}
```
## License

Copyright IBM Corporation 2026

Licensed under the Apache License, Version 2.0

---