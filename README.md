# User Event Sink Connector (Reference Implementation)

A reference implementation in Java demonstrating how to build a connector for sending user events to Moloco MCM's User Event ingestion API endpoint. This implementation showcases best practices for connection pooling, validation, and error handling that you can adapt for your own user event data ingestion services.

## Features Demonstrated

- HTTP connection pooling with configurable limits
- JSON payload validation and null filtering
- Graceful connection management and cleanup
- Support for both String and JsonNode input formats
- Comprehensive error handling and validation

## Implementation Overview

This reference code shows how to:

### Initialize a Connector
```java
UserEventSinkConnector connector = new UserEventSinkConnector(
    "YOUR_PLATFORM_ID",
    "api.example.com",
    "your-api-key",
    100 // maxTotalConnections
);
```

### Send Events
```java
try {
    // Using JSON string
    String jsonString = "{\"event_type\":\"PAGE_VIEW\",\"user_id\":\"123\"}";
    connector.send(jsonString);

    // Using JsonNode
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(jsonString);
    connector.send(jsonNode);
} catch (IOException e) {
    // Handle network or API errors
} catch (IllegalArgumentException e) {
    // Handle validation errors
} finally {
    connector.close();
}
```

## Key Components

### Configuration Parameters

The example implementation handles these key parameters:

- `PLATFORM_ID`: Platform identifier
- `API_HOSTNAME`: API endpoint hostname
- `API_KEY`: User Event API Authentication key
- `MAX_TOTAL_CONNECTIONS`: Connection pool limit (defaults to 100)

### Error Handling

The implementation demonstrates handling of:

- `IllegalArgumentException`: For validation errors
- `IOException`: For network and API communication errors

### Best Practices Demonstrated

1. Connector initialization with configurable connection pooling
2. Sending various event types
3. Retry logic with exponential backoff
4. Proper resource cleanup with `close()` method
5. Error handling for both validation and network errors

## Technical Requirements

To run this reference implementation, you'll need:

- Java 11 or higher
- Jackson library for JSON processing
- Apache HttpComponents Client 5.x

## Building the Example

```bash
./gradlew clean build
```

## Using This Reference

Feel free to:
- Study the implementation patterns
- Copy and modify the code for your needs
- Use it as a starting point for your own connector
- Adapt the error handling and validation logic

## License

© Moloco, Inc. 2024 All rights reserved. Released under Apache 2.0 License

---
**Note**: This is a reference implementation intended to demonstrate best practices. You should adapt and modify this code to meet your specific requirements and security needs.