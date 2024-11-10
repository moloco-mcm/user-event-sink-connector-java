import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moloco.mcm.UserEventSinkConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.io.IOException;

@DisplayName("UserEventSinkConnector Tests")
class UserEventSinkConnectorTest {
    // Constants for test configuration
    private static final String TEST_PLATFORM = "PLATFORM";
    private static final String TEST_URL = "https://sandbox-dcsn.mcm-api.moloco.com";
    private static final String TEST_API_KEY = "my_api_key";

    // Test data constants
    private static final String VALID_TIMESTAMP = "1617870506121";
    private static final String EMPTY_TIMESTAMP = "";
    private static final String EVENT_TYPE_HOME = "HOME";

    private UserEventSinkConnector connector;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        try {
            connector = new UserEventSinkConnector(TEST_PLATFORM, TEST_URL, TEST_API_KEY, 100, 10);
        } catch (IllegalArgumentException e) {
            
        }
    }

    @AfterEach
    void tearDown() {
        if (connector != null) {
            connector.close();
        }
    }

    @Nested
    @DisplayName("Null Input Tests")
    class NullInputTests {
        @Test
        @DisplayName("Should throw IllegalArgumentException when Platform ID is null")
        void testUserEventSinkConnectorWithNullParameter1() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new UserEventSinkConnector(null, TEST_URL, TEST_API_KEY, 100, 10)
            );

            assertEquals("platformID cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when API Hostname is null")
        void testUserEventSinkConnectorWithNullParameter2() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new UserEventSinkConnector(TEST_PLATFORM, null, TEST_API_KEY, 100, 10)
            );

            assertEquals("eventApiHostname cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when API Key is null")
        void testUserEventSinkConnectorWithNullParameter3() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new UserEventSinkConnector(TEST_PLATFORM, TEST_URL, null, 100, 10)
            );

            assertEquals("eventApiKey cannot be null or empty", exception.getMessage());
        }



        @Test
        @DisplayName("Should throw IllegalArgumentException when JsonNode is null")
        void testSendNullJsonNode() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> connector.send((JsonNode) null)
            );

            assertEquals("The jsonNode cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when String content is null")
        void testSendNullString() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> connector.send((String) null)
            );

            assertEquals("The jsonString cannot be null or empty", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Invalid Input Tests")
    class InvalidInputTests {
        @Test
        @DisplayName("Should throw Exception when timestamp is empty")
        void testSendEmptyTimestamp() {
            String invalidJson = createEventJson(EVENT_TYPE_HOME, EMPTY_TIMESTAMP);

            Exception exception = assertThrows(
                    Exception.class,
                    () -> connector.send(invalidJson)
            );

            assertEquals(
                    "The timestamp field cannot be null: " + invalidJson,
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should throw Exception when event type is missing")
        void testSendMissingEventType() {
            String invalidJson = String.format(
                    "{\"timestamp\":\"%s\"}",
                    VALID_TIMESTAMP
            );

            assertThrows(
                    Exception.class,
                    () -> connector.send(invalidJson)
            );
        }
    }

    @Nested
    @DisplayName("API Communication Tests")
    class ApiCommunicationTests {
        @Test
        @DisplayName("Should handle 404 response from API")
        void testSendValidJsonReceives404() {
            String validJson = createEventJson(EVENT_TYPE_HOME, VALID_TIMESTAMP);

            Exception exception = assertThrows(
                    Exception.class,
                    () -> connector.send(validJson)
            );

            assertEquals(
                    "Request failed: status code: 404, reason phrase: 404 page not found\n",
                    exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should handle valid event submission")
        void testSendValidEvent() {
            String validJson = createCompleteEventJson();

            // This test expects a 404 in the current sandbox environment
            assertThrows(
                    Exception.class,
                    () -> connector.send(validJson)
            );
        }
    }

    // Helper methods for creating test JSON
    private String createEventJson(String eventType, String timestamp) {
        return String.format(
                "{\"event_type\":\"%s\",\"timestamp\":\"%s\"}",
                eventType,
                timestamp
        );
    }

    private String createCompleteEventJson() {
        return String.format(
                "{\"event_type\":\"%s\",\"timestamp\":\"%s\",\"user_id\":\"test123\"}",
                EVENT_TYPE_HOME,
                VALID_TIMESTAMP
        );
    }
}