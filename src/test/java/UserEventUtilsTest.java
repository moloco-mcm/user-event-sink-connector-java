import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moloco.mcm.UserEventUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UserEventUtilsTest {
    private ObjectMapper mapper;
    private UserEventUtils utils;

    @BeforeEach
    void setUp() {
        utils = new UserEventUtils();
        mapper = new ObjectMapper();
    }

    @Nested
    class ArrayFilterTests {
        @Test
        void shouldReturnNullWhenInputIsNull() {
            JsonNode result = assertDoesNotThrow(() -> utils.filterArray(null));
            assertNull(result, "Result should be null for null input");
        }

        @Test
        void shouldReturnEmptyArrayForEmptyObjects() throws Exception {
            // Given
            JsonNode input = mapper.readTree("[{},{}]");
            JsonNode expected = mapper.readTree("[]");

            // When
            JsonNode result = utils.filterArray(input);

            // Then
            assertEquals(expected, result, "Should return empty array for array of empty objects");
        }
    }

    @Nested
    class NullFilterTests {
        @Test
        void shouldHandleNullInput() {
            ObjectNode result = assertDoesNotThrow(() -> utils.filterNulls(null));
            assertNull(result, "Should return null for null input");
        }

        @Test
        void shouldFilterNullValues() throws Exception {
            // Using simple array of Object[] instead of record
            Object[][] testCases = {
                    new Object[]{
                            "{\"key1\":\"value1\"}",
                            "{\"key1\":\"value1\"}"
                    },
                    new Object[]{
                            "{\"key1\":\"value1\",\"key2\":null}",
                            "{\"key1\":\"value1\"}"
                    },
                    new Object[]{
                            "{\"key1\":\"value1\",\"key2\":[\"v1\",null]}",
                            "{\"key1\":\"value1\",\"key2\":[\"v1\"]}"
                    },
                    new Object[]{
                            "{\"key1\":[{\"key11\":\"val11\",\"key12\":null}]}",
                            "{\"key1\":[{\"key11\":\"val11\"}]}"
                    },
                    new Object[]{
                            "{\"key1\":{\"key11\":\"value11\",\"key12\":{}}}",
                            "{\"key1\":{\"key11\":\"value11\"}}"
                    }
            };

            for (Object[] testCase : testCases) {
                String input = (String) testCase[0];
                String expected = (String) testCase[1];

                JsonNode inputNode = mapper.readTree(input);
                JsonNode expectedNode = mapper.readTree(expected);
                JsonNode result = utils.filterNulls(inputNode);

                assertEquals(
                        expectedNode,
                        result,
                        String.format("Failed for input: %s", input)
                );
            }
        }
    }

    @Nested
    class ValidationTests {
        @ParameterizedTest
        @ValueSource(
                strings = {
                        "{\"event_type\":\"HOME\",\"timestamp\":\"1617870506121\"}",
                        "{\"event_type\":\"LAND\",\"timestamp\":\"1617870506121\"}",
                        "{\"event_type\":\"ITEM_PAGE_VIEW\",\"timestamp\":\"1617870506121\",\"items\":[{}]}",
                        "{\"event_type\":\"ADD_TO_CART\",\"timestamp\":\"1617870506121\",\"items\":[{}]}",
                        "{\"event_type\":\"ADD_TO_WISHLIST\",\"timestamp\":\"1617870506121\",\"items\":[{}]}",
                        "{\"event_type\":\"SEARCH\",\"timestamp\":\"1617870506121\",\"search_query\":\"test\"}",
                        "{\"event_type\":\"PAGE_VIEW\",\"timestamp\":\"1617870506121\",\"page_id\":\"test\"}",
                        "{\"event_type\":\"PURCHASE\",\"timestamp\":\"1617870506121\",\"items\":[{}]}"
                })
        void shouldValidateValidData(String input) {
            JsonNode jsonInput = assertDoesNotThrow(() -> mapper.readTree(input));
            assertDoesNotThrow(() -> utils.validateData(jsonInput));
        }

        @Test
        void shouldRejectNullInput() {
            JsonNode jsonInput = null;

            Exception exception = assertThrows(
                    Exception.class,
                    () -> utils.validateData(jsonInput)
            );

            assertEquals(
                    "jsonNode parameter cannot be null",
                    exception.getMessage()
            );
        }

        @Test
        void shouldRejectMissingTimestamp() {
            String input = "{}";
            JsonNode jsonInput = assertDoesNotThrow(() -> mapper.readTree(input));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> utils.validateData(jsonInput)
            );

            assertEquals(
                    "The timestamp field must be present: {}",
                    exception.getMessage()
            );
        }

        @Test
        void shouldRejectInvalidTimestamp() {
            String input = "{\"timestamp\":\"2024-11-03\"}";
            JsonNode jsonInput = assertDoesNotThrow(() -> mapper.readTree(input));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> utils.validateData(jsonInput)
            );

            assertEquals(
                    "The timestamp field must be a Unix timestamp in milliseconds (not seconds) indicating when the event occurred (e.g., 1617870506121): {\"timestamp\":\"2024-11-03\"}",
                    exception.getMessage()
            );
        }

        @Test
        void shouldRejectMissingEventType() {
            String input = "{\"timestamp\":\"1617870506121\"}";
            JsonNode jsonInput = assertDoesNotThrow(() -> mapper.readTree(input));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> utils.validateData(jsonInput)
            );

            assertEquals(
                    "The event_type field is missing",
                    exception.getMessage()
            );
        }

        @Test
        void shouldRejectInvalidEventType() {
            String input = "{\"event_type\":\"INVALID_TYPE\",\"timestamp\":\"1617870506121\"}";
            JsonNode jsonInput = assertDoesNotThrow(() -> mapper.readTree(input));

            Exception exception = assertThrows(
                    Exception.class,
                    () -> utils.validateData(jsonInput)
            );

            assertEquals(
                    "Unknown event type: INVALID_TYPE",
                    exception.getMessage()
            );
        }
    }

    @Nested
    class EventTypeTests {
        private class TestCase {
            final String input;
            final boolean shouldPass;

            TestCase(String input, boolean shouldPass) {
                this.input = input;
                this.shouldPass = shouldPass;
            }
        }

        @Test
        void shouldValidateHomeEvent() {
            assertDoesNotThrow(() -> utils.testHome(null));
        }

        @Test
        void shouldValidateItemPageViewEvent() throws Exception {
            TestCase[] testCases = {
                    new TestCase("{\"items\":null}", false),
                    new TestCase("{\"items\":{}}", false),
                    new TestCase("{\"items\":[]}", false),
                    new TestCase("{\"items\":[{}]}", true)
            };

            for (TestCase testCase : testCases) {
                JsonNode input = mapper.readTree(testCase.input);

                if (testCase.shouldPass) {
                    assertDoesNotThrow(() -> utils.testPDP(input));
                } else {
                    assertThrows(Exception.class, () -> utils.testPDP(input));
                }
            }
        }

        @Test
        void shouldValidatePageViewEvent() throws Exception {
            TestCase[] testCases = {
                    new TestCase("{\"page_id\":\"aaaa\"}", true),
                    new TestCase("{\"page_id\":\"\"}", false),
                    new TestCase("{\"page_id\":null}", false),
                    new TestCase("{}", false)
            };

            for (TestCase testCase : testCases) {
                JsonNode input = mapper.readTree(testCase.input);

                if (testCase.shouldPass) {
                    assertDoesNotThrow(() -> utils.testPageView(input));
                } else {
                    assertThrows(Exception.class, () -> utils.testPageView(input));
                }
            }
        }

        @Test
        void shouldValidateSearchEvent() throws Exception {
            TestCase[] testCases = {
                    new TestCase("{\"search_query\":\"aaaa\"}", true),
                    new TestCase("{\"search_query\":\"\"}", false),
                    new TestCase("{\"search_query\":null}", false),
                    new TestCase("{}", false)
            };

            for (TestCase testCase : testCases) {
                JsonNode input = mapper.readTree(testCase.input);

                if (testCase.shouldPass) {
                    assertDoesNotThrow(() -> utils.testSearch(input));
                } else {
                    assertThrows(Exception.class, () -> utils.testSearch(input));
                }
            }
        }

        @Test
        void shouldValidatePurchaseEvent() throws Exception {
            TestCase[] testCases = {
                    new TestCase(
                            "{\"items\":[{\"id\":\"396172\",\"price\":{\"currency\":\"USD\",\"amount\":\"429.98\"},\"quantity\":\"1\"}]}",
                            true
                    ),
                    new TestCase("{\"items\":[]}", false),
                    new TestCase("{\"items\":null}", false),
                    new TestCase("{\"items\":{}}", false)
            };

            for (TestCase testCase : testCases) {
                JsonNode input = mapper.readTree(testCase.input);

                if (testCase.shouldPass) {
                    assertDoesNotThrow(() -> utils.testPurchase(input));
                } else {
                    assertThrows(Exception.class, () -> utils.testPurchase(input));
                }
            }
        }
    }
}