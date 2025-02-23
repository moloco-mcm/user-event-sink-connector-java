/*
 * Copyright 2024 Moloco, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.databind.JsonNode;
import com.moloco.mcm.UserEventSinkConnector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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


    @BeforeEach
    void setUp() {
        try {
            connector = new UserEventSinkConnector.Builder()
                    .platformID(TEST_PLATFORM)
                    .eventApiHostname(TEST_URL)
                    .eventApiKey(TEST_API_KEY)
                    .retryMaxAttempts(1)
                    .retryExponentialBackoffMultiplier(1)
                    .retryDelayMilliseconds(100).build();
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
                    () -> new UserEventSinkConnector.Builder().platformID(null)
            );

            assertEquals("platformID cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when Platform ID is empty")
        void testUserEventSinkConnectorWithNullParameter2() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new UserEventSinkConnector.Builder().platformID("")
            );

            assertEquals("platformID cannot be null or empty", exception.getMessage());
        }


        @Test
        @DisplayName("Should throw IllegalArgumentException when API Hostname is null")
        void testUserEventSinkConnectorWithNullParameter3() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new UserEventSinkConnector.Builder().eventApiHostname(null)
            );

            assertEquals("eventApiHostname cannot be null or empty", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when API Key is null")
        void testUserEventSinkConnectorWithNullParameter4() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new UserEventSinkConnector.Builder().eventApiKey(null)
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

        @Test
        @DisplayName("Should throw IllegalArgumentException when String content is empty")
        void testSendEmptyString() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> connector.send("")
            );

            assertEquals("The jsonString cannot be null or empty", exception.getMessage());
        }

    }

    @Nested
    @DisplayName("Invalid Input Tests")
    class InvalidInputTests {
        @Test
        @DisplayName("Should throw IllegalArgumentException when maxTotalConnections is zero(0)")
        void testZeroMaxTotalConnections() {
            Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserEventSinkConnector.Builder().maxTotalConnections(0)
            );

            assertEquals(
                "maxTotalConnections must be at least one(1)",
                exception.getMessage()
            );
        }


        @Test
        @DisplayName("Should throw IllegalArgumentException when maxTotalConnections is zero(0)")
        void testRetryMaxAttempts1() {
            Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserEventSinkConnector.Builder().retryMaxAttempts(0)
            );

            assertEquals(
                "retryMaxAttempts must be between 1 and 10",
                exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when maxTotalConnections is 11")
        void testRetryMaxAttempts2() {
            Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserEventSinkConnector.Builder().retryMaxAttempts(11)
            );

            assertEquals(
                "retryMaxAttempts must be between 1 and 10",
                exception.getMessage()
            );
        }


        @Test
        @DisplayName("Should pass when retryMaxAttempts is 1")
        void testRetryMaxAttempts3() {
            new UserEventSinkConnector.Builder().retryMaxAttempts(1);
        }


        @Test
        @DisplayName("Should pass when retryMaxAttempts is 10")
        void testRetryMaxAttempts4() {
            new UserEventSinkConnector.Builder().retryMaxAttempts(10);
        }


        @Test
        @DisplayName("Should throw IllegalArgumentException when retryMaxAttempts is less than 1")
        void testRetryMaxAttempts5() {
            Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserEventSinkConnector.Builder().retryMaxAttempts(0)
            );

            assertEquals(
                "retryMaxAttempts must be between 1 and 10",
                exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should pass when maxTotalConnections is 1")
        void testMaxTotalConnections1() {
            new UserEventSinkConnector.Builder().maxTotalConnections(1);
        }



        @Test
        @DisplayName("Should pass when retryExponentialBackoffMultiplier is 1")
        void testRetryExponentialBackoffMultiplier1() {
            new UserEventSinkConnector.Builder().retryExponentialBackoffMultiplier(1);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when retryExponentialBackoffMultiplier is less than one(1)")
        void testRetryExponentialBackoffMultiplier2() {
            Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserEventSinkConnector.Builder().retryExponentialBackoffMultiplier(0)
            );

            assertEquals(
                "retryExponentialBackoffMultiplier must be at least 1",
                exception.getMessage()
            );
        }

        @Test
        @DisplayName("Should pass when retryDelayMilliseconds is 1")
        void retryDelayMilliseconds1() {
            new UserEventSinkConnector.Builder().retryDelayMilliseconds(1);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when retryDelayMilliseconds is less than one(1)")
        void retryDelayMilliseconds2() {
            Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserEventSinkConnector.Builder().retryDelayMilliseconds(0)
            );

            assertEquals(
                "retryDelayMilliseconds must be at least one(1)",
                exception.getMessage()
            );
        }


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