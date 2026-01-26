# z/OS Connect File System Logger Interceptor

A sample z/OS Connect interceptor that captures API provider and API requester data and writes it in a JSON format to a log file on the file system.

## Overview

This interceptor sample provides comprehensive logging capabilities for z/OS Connect, capturing:
- Request and response headers
- Request and response bodies (optional)
- Request and response sizes (in bytes)
- Query strings
- System of Record (SoR) details for API provider
- API endpoint details for API requester
- HTTP status codes

The interceptor writes structured JSON log entries to rolling log files with configurable size and retention limits.

## Features

- **Dual Interceptor Support**: Separate interceptors for API provider and API requester
- **Rolling File Logs**: Automatic log rotation based on file size
- **Configurable Retention**: Control maximum number of log files to retain
- **JSON Format**: Structured log entries in JSON format for easy parsing
- **Optional Body Logging**: Enable/disable request and response body logging
- **Thread-Safe**: Safe for concurrent use across multiple APIs and API requesters
- **Minimal Dependencies**: No external JSON libraries required

## Project Structure

```
sample-fslogger-interceptor/
├── build.gradle                                       # Root Gradle build file
├── settings.gradle                                    # Gradle settings
├── com.ibm.sample.zosconnect.fslogger.interceptor/    # OSGi Bundle
│   ├── build.gradle                                    # Interceptor module build
│   ├── src/
│   │   └── com/ibm/sample/zosconnect/fslogger/
│   │       ├── common/                                 # Shared utilities
│   │       │   └── RollingFileHandler.java
│   │       ├── provider/                               # API provider interceptor
│   │       │   ├── FileSystemLoggerProviderInterceptor.java
│   │       │   └── LogEntryProvider.java
│   │       └── requester/                              # API requester interceptor
│   │           ├── FileSystemLoggerRequesterInterceptor.java
│   │           └── LogEntryRequester.java
│   └── BundleContent/
│       ├── META-INF/
│       │   └── MANIFEST.MF
│       └── OSGI-INF/
│           ├── metatype/
│           │   └── metatype.xml
│           ├── com.ibm.sample.zosconnect.fslogger.provider.xml
│           └── com.ibm.sample.zosconnect.fslogger.requester.xml
└── com.ibm.sample.zosconnect.fslogger.feature/        # Liberty Feature
    ├── build.gradle                                    # Feature module build
    └── OSGI-INF/
        └── SUBSYSTEM.MF
```

## Building

### Prerequisites

- Java 8 or later
- Gradle 8.5 or later
- z/OS Connect SPI JAR

### Installing the z/OS Connect SPI JAR

The z/OS Connect SPI JAR is required for compilation but is not available in public Maven repositories. You must install it manually from your z/OS Connect installation.

The SPI JAR is typically located at:
```
<zosconnect_install>/dev/com.ibm.zosconnect.spi.jar
```
Install it to your local Maven repository:
```bash
mvn install:install-file \
  -Dfile=/path/to/com.ibm.zosconnect.spi.jar \
  -DgroupId=com.ibm.zosconnect \
  -DartifactId=com.ibm.zosconnect.spi \
  -Dversion=2.0.0.0 \
  -Dpackaging=jar
```

**Note**: Gradle will use the local Maven repository (`~/.m2/repository`) to resolve this dependency.

### Building with Gradle

Once the z/OS Connect SPI JAR is installed, build the project using Gradle:

```bash
gradle clean build
```

### Build Output

After a successful build, you'll find:

1. **OSGi Bundle JAR**
   ```
   com.ibm.sample.zosconnect.fslogger.interceptor/build/libs/com.ibm.sample.zosconnect.fslogger.interceptor-1.0.0.0.jar
   ```

2. **ESA File (Liberty Feature)**
   ```
   com.ibm.sample.zosconnect.fslogger.feature/build/distributions/fslogger-1.0.esa
   ```

The ESA file is ready to install.

## Testing

A comprehensive test suite is included to ensure code quality and reliability. The test suite uses JUnit 5 and Mockito.

### Running Tests

Run all tests:
```bash
gradle test
```

Run specific test class:
```bash
gradle test --tests RollingFileHandlerTest
```

View test report:
```bash
# After running tests, open:
# build/reports/tests/test/index.html
```

### Test Coverage

The test suite includes:
- **RollingFileHandlerTest**: File rotation, cleanup, thread safety
- **LogEntryProviderTest**: JSON serialization, data capture for API Provider
- **LogEntryRequesterTest**: JSON serialization, data capture for API Requester
- **FileSystemLoggerProviderInterceptorTest**: Provider interceptor lifecycle and behavior
- **FileSystemLoggerRequesterInterceptorTest**: Requester interceptor lifecycle and behavior

For detailed test documentation, see [test/README.md](com.ibm.sample.zosconnect.fslogger.interceptor/test/README.md)

### Tests Run Automatically

Tests are automatically executed during the build process:
```bash
gradle build
```

The build will fail if any tests fail, ensuring code quality.

## Installing the Feature

1. Transfer the ESA file to a directory on your z/OS LPAR:

   ```bash
   /u/user/zosconnect/extension/fslogger-1.0.esa
   ```

2. Configure the Liberty user directory (if not already set):
   ```bash
   export WLP_USER_DIR=/var/zosconnect
   ```

3. Install using featureUtility:
   ```bash
   <zosconnect_install>/wlp/bin/featureUtility install /u/user/zosconnect/extension/fslogger-1.0.esa
   ```
## Configuration

### API provider Interceptor

Add the following to your `server.xml`:

```xml
<featureManager>
    <feature>zosconnect:zosConnect-3.0</feature>
    <feature>zosconnect:monitoring-1.0</feature> 
    <feature>usr:fslogger-1.0</feature>
</featureManager>

<zosconnect_monitoring apiRequesterInterceptorsRef="interceptorList"/>
<zosconnect_zosConnectInterceptors id="interceptorList" interceptorRef="fileSystemProvider"/> 

<usr_fileSystemLoggerProvider
    id="fileSystemProvider"
    logDirectory="/u/user/zosconnect/logs"
    maxFileSize="10485760"
    maxFileCount="10"
    requestHeaders="Content-Type,Authorization"
    responseHeaders="Content-Type,X-Custom-Header"
    includeBody="false"/>
```

### API requester Interceptor

```xml
<featureManager>
    <feature>zosconnect:zosConnect-3.0</feature>
    <feature>zosconnect:monitoring-1.0</feature> 
    <feature>usr:fslogger-1.0</feature>
</featureManager>

<zosconnect_monitoring apiRequesterInterceptorsRef="interceptorList"/>
<zosconnect_zosConnectInterceptors id="interceptorList" interceptorRef="fileSystemRequester"/> 

<usr_fileSystemLoggerRequester
    id="fileSystemRequester"
    logDirectory="/u/user/zosconnect/logs"
    maxFileSize="10485760"
    maxFileCount="10"
    requestHeaders="Content-Type,Authorization"
    responseHeaders="Content-Type,Cache-Control"
    includeBody="false"/>
```

### Configuration Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `id` | String | Yes | - | A unique identifier for this Interceptor |
| `sequence` | Integer | No | 0 | Interceptor execution order (0-2147483647) |
| `logDirectory` | String | Yes | - | Directory path where log files will be written |
| `maxFileSize` | Long | No | 10485760 | Maximum size of each log file in bytes (10MB default) |
| `maxFileCount` | Integer | No | 10 | Maximum number of log files to retain |
| `requestHeaders` | String | No | "" | Comma-separated list of request header names to capture (e.g., Content-Type,Authorization). |
| `responseHeaders` | String | No | "" | Comma-separated list of response header names to capture (e.g., Content-Type,X-Custom-Header). |
| `includeBody` | Boolean | No | false | Include request and response bodies in log entries |

## Log File Format

### API provider Log Entry

```json
{
  "timestamp": "2026-01-14T10:15:30.123Z",
  "requestId": 123456789,
  "user": "user1",
  "mappedUser": "USERID",
  "request": {
    "uri": "/api/v1/customers",
    "method": "POST",
    "queryString": "filter=active&limit=10",
    "headers": {
      "Content-Type": "application/json",
      "Accept": "application/json"
    },
    "body": "{\"customerId\":\"12345\"}",
    "size": 23
  },
  "sor": {
    "identifier": "CICS01",
    "resource": "CUSTOMER",
    "reference": "REF123"
  },
  "response": {
    "statusCode": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{\"status\":\"success\"}",
    "size": 21
  }
}
```

**Note**: The `size` field is always included (when bodies exist) and represents the size in bytes (UTF-8 encoding), even when `includeBody` is set to `false`. The `body` field is only included when `includeBody` is `true`.

### API Requester Log Entry

```json
{
  "timestamp": "2026-01-14T10:15:30.123Z",
  "requestId": 987654321,
  "user": "user1",
  "mappedUser": "USERID",
  "request": {
    "requestType": "CICS",
    "jobname": "CICSJOB",
    "jobid": "JOB00123",
    "sysname": "SYSA",
    "cics": {
      "applid": "CICS01",
      "taskNumber": 54321,
      "transid": "ABCD"
    },
    "headers": {
      "Content-Type": "application/json",
      "Accept": "application/json"
    },
    "body": "{\"query\":\"customerId\"}",
    "size": 24
  },
  "endpoint": {
    "host": "backend.example.com",
    "port": 8080,
    "method": "GET",
    "path": "/customers/12345",
    "queryString": "format=json&include=details"
  },
  "response": {
    "statusCode": 200,
    "headers": {
      "Content-Type": "application/json",
      "Cache-Control": "no-cache"
    },
    "body": "{\"customer\":{\"id\":\"12345\"}}",
    "size": 30
  }
}
```

**Note**: The `requestType` field within the `request` object indicates the type of application making the request (e.g., "CICS", "IMS", "ZOS"). The z/OS system information (`jobname`, `jobid`, `sysname`) is included directly within the `request` object. The `cics` object is only included when `requestType` is "CICS", and the `ims` object is only included when `requestType` is "IMS".

#### API Requester Log Entry with IMS

```json
{
  "timestamp": "2026-01-14T10:15:30.123Z",
  "requestId": 987654321,
  "user": "user1",
  "mappedUser": "USERID",
  "request": {
    "requestType": "IMS",
    "jobname": "IMSJOB",
    "jobid": "JOB00456",
    "sysname": "SYSB",
    "ims": {
      "identifier": "IMS01",
      "regionId": 2,
      "transname": "IMSTRAN",
      "appname": "IMSAPP",
      "psbname": "IMSPSB"
    },
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{\"query\":\"customerId\"}",
    "size": 24
  },
  "endpoint": {
    "host": "backend.example.com",
    "port": 8080,
    "method": "GET",
    "path": "/customers/12345"
  },
  "response": {
    "statusCode": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "body": "{\"customer\":{\"id\":\"12345\"}}",
    "size": 30
  }
}
```

**Note**: The `size` field is always included (when bodies exist) in both request and response objects and represents the size in bytes (UTF-8 encoding), even when `includeBody` is set to `false`. The `body` field is only included when `includeBody` is `true`.

## Log File Naming

Log files are created with the interceptor ID and timestamps in the format:
```
zosconnect-<id>-YYYYMMdd-HHmmss.log
```

Where `<id>` is the unique identifier specified in the interceptor configuration.

Examples:
- `zosconnect-fileSystemProvider-20260114-101530.log`
- `zosconnect-fileSystemRequester-20260114-101530.log`

**Note**: Each interceptor instance manages its own set of log files based on its ID. This allows multiple interceptor instances to write to the same directory without interfering with each other's file rotation and cleanup.

## Performance Considerations

- **Body Logging**: Enabling `includeBody` can significantly increase log file size and I/O overhead. Use with caution in production environments.
- **Size Tracking**: Body sizes are always captured (even when `includeBody` is `false`) with minimal performance impact, providing valuable metrics without storing full body content.
- **File Size**: Larger `maxFileSize` values reduce the frequency of file rotation but increase individual file sizes.
- **File Count**: Higher `maxFileCount` values consume more disk space but provide longer retention history.
- **Disk I/O**: The interceptor uses buffered writes and flushes after each log entry to ensure data integrity.

## Security Considerations

- **Sensitive Data**: Be cautious when enabling `includeBody` as it may log sensitive information such as passwords, credit card numbers, or personal data.
- **File Permissions**: Ensure the log directory has appropriate permissions to prevent unauthorized access.
- **Log Rotation**: Implement external log rotation or archival processes for long-term retention and compliance.

## Troubleshooting

### Interceptor Not Activating

Check the Liberty `messages.log` for activation messages:
```
FileSystemLoggerProviderInterceptor activated
FileSystemLoggerProviderInterceptor initialized with logDirectory=...
```

### Log Files Not Created

1. Verify the `logDirectory` exists and is writable
2. Check Liberty has permissions to create files in the directory
3. Review `messages.log` for error messages

### Log Files Not Rotating

1. Verify `maxFileSize` is set appropriately
2. Check disk space availability
3. Ensure `maxFileCount` is greater than 1

## License

Copyright IBM Corporation 2026

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Support

This is sample code and is not officially supported by IBM. Use at your own risk.

Please raise an issue in this repository if you encounter a problem using the sample code.

## Version History

- **1.0.0** (2026-01-15)
  - Initial release
  - API provider interceptor
  - API requester interceptor
  - Rolling file handler
