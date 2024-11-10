import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        void shouldReturnArrayofEmptyObjectsAsIs() throws Exception {
            // Given
            JsonNode input = mapper.readTree("[{},{}]");
            JsonNode expected = mapper.readTree("[{},{}]");

            // When
            JsonNode result = utils.filterArray(input);

            // Then
            assertEquals(expected, result, "Should return array of empty objects as is");
        }
    }

    @Nested
    class NullFilterTests {
        @Test
        void shouldHandleNullInput() {
            JsonNode result = assertDoesNotThrow(() -> utils.filterNulls(null));
            assertNull(result, "Should return null for null input");
        }

        @Test
        void shouldFilterNullValues() throws Exception {
            // Using simple array of Object[] instead of record
            Object[][] testCases = {
                // Basic null value filtering
                new Object[]{
                    "{\"key\":null}",
                    "{}"
                },
                new Object[]{
                    "{\"key1\":\"value1\",\"key2\":null}",
                    "{\"key1\":\"value1\"}"
                },
                
                // Nested objects with nulls
                new Object[]{
                    "{\"obj\":{\"a\":1,\"b\":null,\"c\":{\"d\":null}}}",
                    "{\"obj\":{\"a\":1,\"c\":{}}}"
                },
                new Object[]{
                    "{\"a\":{\"b\":{\"c\":null}}}",
                    "{\"a\":{\"b\":{}}}"
                },
                
                // Arrays with nulls
                new Object[]{
                    "{\"arr\":[1,null,2,null,3]}",
                    "{\"arr\":[1,2,3]}"
                },
                new Object[]{
                    "[null,{\"a\":1},null,{\"b\":null}]",
                    "[{\"a\":1},{}]"
                },
                
                // Mixed nested structures
                new Object[]{
                    "{\"obj\":{\"arr\":[1,null,{\"a\":null}]}}",
                    "{\"obj\":{\"arr\":[1,{}]}}"
                },
                new Object[]{
                    "[{\"a\":1,\"b\":null},[1,null],{\"c\":null}]",
                    "[{\"a\":1},[1],{}]"
                },
                
                // Edge cases
                new Object[]{
                    "\"string\"",
                    "\"string\""
                },
                new Object[]{
                    "123",
                    "123"
                },
                new Object[]{
                    "true",
                    "true"
                },
                new Object[]{
                    "false",
                    "false"
                },
                new Object[]{
                    "[]",
                    "[]"
                },
                new Object[]{
                    "{}",
                    "{}"
                },
                
                // Special values
                new Object[]{
                    "{\"nullString\":\"null\"}",
                    "{\"nullString\":\"null\"}"
                },
                new Object[]{
                    "{\"empty\":\"\"}",
                    "{\"empty\":\"\"}"
                },
                
                // Complex nested structures
                new Object[]{
                    "{\"a\":[{\"b\":null,\"c\":[1,null,{\"d\":null}]}]}",
                    "{\"a\":[{\"c\":[1,{}]}]}"
                },
                new Object[]{
                    "{\"deep\":{\"nested\":{\"array\":[1,null,{\"obj\":null}],\"value\":null}}}",
                    "{\"deep\":{\"nested\":{\"array\":[1,{}]}}}"
                },
                
                // Multiple nested arrays
                new Object[]{
                    "[[null,[1,null]],[null,2],null]",
                    "[[[1]],[2]]"
                },
                
                // Mixed types
                new Object[]{
                    "{\"str\":\"value\",\"num\":123,\"bool\":true,\"null\":null,\"arr\":[1,null],\"obj\":{\"a\":null}}",
                    "{\"str\":\"value\",\"num\":123,\"bool\":true,\"arr\":[1],\"obj\":{}}"
                },
                
                // Empty structures
                new Object[]{
                    "{\"emptyArr\":[],\"emptyObj\":{},\"null\":null}",
                    "{\"emptyArr\":[],\"emptyObj\":{}}"
                },
                
                // Arrays of empty structures
                new Object[]{
                    "[{},{},null,{}]",
                    "[{},{},{}]"
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