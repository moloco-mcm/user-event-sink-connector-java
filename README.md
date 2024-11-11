# User Event Sink Connector

A Java library for sending user events to a specified API endpoint with built-in connection pooling, validation, and error handling.

## Features

- HTTP connection pooling with configurable limits
- JSON payload validation and null filtering
- Graceful connection management and cleanup
- Support for both String and JsonNode input formats
- Comprehensive error handling and validation

## Installation

Add the following dependency to your project:

### Maven
```xml
<dependency>
    <groupId>com.moloco.mcm</groupId>
    <artifactId>user-event-sink-connector</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("com.moloco.mcm:user-event-sink-connector:0.1.0")
}
```

## Usage

### Basic Implementation

```java
// Initialize the connector
UserEventSinkConnector connector = new UserEventSinkConnector(
    "YOUR_PLATFORM_ID",
    "api.example.com",
    "your-api-key",
    100, // maxTotalConnections
    10 // maxConnectionsPerRoute
);
try {
    // Send event using JSON string
    String jsonString = "{\"event_type\":\"PAGE_VIEW\",\"user_id\":\"123\"}";
    connector.send(jsonString);
    // Or send event using JsonNode
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(jsonString);
    connector.send(jsonNode);
} catch (IOException e) {
    // Handle network or API errors
} catch (IllegalArgumentException e) {
    // Handle validation errors
} finally {
    connector.close(); // Always close the connector when done
}
```

### Configuration Options

The connector accepts the following parameters:

- `platformID`: Your platform identifier (required)
- `eventApiHostname`: API endpoint hostname (required)
- `eventApiKey`: Authentication key for the API (required)
- `maxTotalConnections`: Maximum number of concurrent connections (defaults to 100)
- `maxConnectionsPerRoute`: Maximum connections per route (defaults to 10)

### Error Handling

The connector throws two types of exceptions:

- `IllegalArgumentException`: For validation errors (null/empty inputs)
- `IOException`: For network and API communication errors

### Best Practices

1. Always close the connector using the `close()` method when done
2. Implement proper error handling for both validation and network errors
3. Configure connection pool sizes based on your application's needs
4. Use try-with-resources or try-finally blocks to ensure proper resource cleanup

## Dependencies

- Jackson (`com.fasterxml.jackson.databind`)
- Apache HttpComponents Client 5.x
- Java 11 or higher

## Building

```bash
./gradlew clean build
```


## License

© Moloco, Inc. 2024 All rights reserved. Released under Apache 2.0 License
