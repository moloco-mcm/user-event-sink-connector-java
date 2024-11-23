# User Event Sink Connector (Reference Implementation)

This Java reference implementation demonstrates how to build a connector for sending user events to Moloco MCM’s User Event ingestion API, showcasing best practices for connection pooling, exponential backoff retries, validation, and error handling, which can be adapted for your own user event data ingestion services.

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
    // Reach out to the Moloco MCM team to get yours!
    "YOUR_PLATFORM_ID", "api.example.com", "your-api-key")
    .maxTotalConnections(16)
    .retryMaxAttempts(4)
    .retryExponentialBackoffMultiplier(2)
    .retryDelaySeconds(1);

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
} catch (IllegalArgumentException | ParseException | IOException | InterruptedException e) {
    // Handle errors
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
- `MAX_TOTAL_CONNECTIONS`: Connection pool limit (defaults to 16)
- `RETRY_MAX_ATTEMPTS`: The maximum number of retry attempts (defaults to 4)
- `RETRY_EXPONENTIAL_BACKOFF_MULTIPLIER`: The multiplier for exponential backoff (defaults to 2)
- `RETRY_DELAY_SECONDS`: The delay in seconds for the first retry attempt (defaults to 1)

### Error Handling

The implementation demonstrates handling of:

- `IllegalArgumentException`: if any argument is invalid, entity is null or if content length > Integer.MAX_VALUE
- `ParseException`: if header elements cannot be parsed
- `IOException`: if an error occurs reading the input stream
- `InterruptedException`: if any thread has interrupted the current thread. The _interrupted status_ of the current thread is cleared when this exception is thrown.

### Best Practices Demonstrated

1. Connector initialization with configurable connection pooling
2. Sending various event types
4. Proper resource cleanup with `close()` method
5. Error handling for various errors

## Technical Requirements

To run this reference implementation, you'll need:

- Java 8 or higher
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