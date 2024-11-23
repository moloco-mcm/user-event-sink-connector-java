package com.moloco.mcm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * A Utility class of the library.
 */
public class UserEventUtils {

    /**
     * Functional interface for a consumer that can throw an exception.
     * This interface allows for lambda expressions or method references
     * that accept a parameter and may throw a checked exception.
     *
     * @param <T> The type of the input to the operation
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        /**
         * Performs this operation on the given argument, allowing for checked exceptions.
         *
         * @param t the input argument of type T
         * @throws Exception if an error occurs during the operation
         */
        void accept(T t) throws IllegalArgumentException;
    }

    private final Map<String, ThrowingConsumer<JsonNode>> testActions = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper instance

    /**
     * Initializes the User Event data validation methods.
     * It stores the map with type codes and the corresponding validation methods for faster data processing.
     */
    public UserEventUtils() {
        // Initialize the map with type codes and their corresponding methods
        testActions.put("HOME", this::testHome);
        testActions.put("LAND", this::testHome);
        testActions.put("ITEM_PAGE_VIEW", this::testPDP);
        testActions.put("ADD_TO_CART", this::testPDP);
        testActions.put("ADD_TO_WISHLIST", this::testPDP);
        testActions.put("SEARCH", this::testSearch);
        testActions.put("PAGE_VIEW", this::testPageView);
        testActions.put("PURCHASE", this::testPurchase);
    }

    /**
     * Validates the User Event data and raises an Exception if invalid content is detected.
     * This validation is not exhaustive; it aims to catch common mistakes early.
     * The Moloco MCM team continually identifies frequent errors to improve validation rules
     * and release updated library versions.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void validateData(JsonNode jsonNode) throws IllegalArgumentException {
        if (jsonNode == null) {
            throw new IllegalArgumentException("The jsonNode cannot be null");
        }

        // Test the common fields
        this.testCommon(jsonNode);

        // Get the action associated with the typeCode or provide a default if not found
        JsonNode eventTypeNode = jsonNode.get("event_type");
        if (null == eventTypeNode) {
            throw new IllegalArgumentException("The event_type field is missing");
        }
        String eventType = eventTypeNode.asText();
        ThrowingConsumer<JsonNode> action = testActions.getOrDefault(
                eventType,
                data -> { throw new IllegalArgumentException("Unknown event type: " + eventType); }
        );
        // Execute the action with the input
        action.accept(jsonNode);
    }

    /**
     * Validates common field values, such as the timestamp.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void testCommon(JsonNode jsonNode) throws IllegalArgumentException {
        if (jsonNode == null) {
            throw new IllegalArgumentException("jsonNode parameter cannot be null");
        }
        JsonNode node = jsonNode.get("timestamp");
        if (node == null) {
            throw new IllegalArgumentException("The timestamp field must be present: " + jsonNode);
        }

        String timestamp = node.asText();
        if (timestamp.isEmpty()) {
            throw new IllegalArgumentException("The timestamp field cannot be null: " + jsonNode);
        }
        try {
            Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The timestamp field must be a Unix timestamp in milliseconds (not seconds) indicating when the event occurred (e.g., 1617870506121): " + jsonNode);
        }
    }

    /**
     * Validates HOME, and LAND User Event data.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void testHome(JsonNode jsonNode) throws IllegalArgumentException {
        // To add validation logics
    }

    /**
     * Validates ITEM_PAGE_VIEW, ADD_TO_CART, and ADD_TO_WISHLIST User Event data.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void testPDP(JsonNode jsonNode) throws IllegalArgumentException {
        JsonNode itemsNode = jsonNode.get("items");
        if (itemsNode == null || !itemsNode.isArray()) {
            throw new IllegalArgumentException("The items field must be a valid array for the following events: ITEM_PAGE_VIEW, ADD_TO_CART, and ADD_TO_WISHLIST. " + jsonNode);
        }

        ArrayNode items = (ArrayNode) itemsNode;
        if (items.isEmpty()) {
            throw new IllegalArgumentException("The items field must not be empty for the following events: ITEM_PAGE_VIEW, ADD_TO_CART, and ADD_TO_WISHLIST. " + jsonNode);
        }
    }

    /**
     * Validates SEARCH User Event data.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void testSearch(JsonNode jsonNode) throws IllegalArgumentException {
        JsonNode node = jsonNode.get("search_query");
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("The search_query field must be present in the SEARCH user event. " + jsonNode);
        }

        String searchQuery = node.asText();
        if (searchQuery.isEmpty()) {
            throw new IllegalArgumentException("The search_query field cannot be null or empty in the SEARCH user event. " + jsonNode);
        }
    }

    /**
     * Validates PAGE_VIEW User Event data.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void testPageView(JsonNode jsonNode) throws IllegalArgumentException {
        JsonNode node = jsonNode.get("page_id");
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("The page_id field must be present in the PAGE_VIEW user event. " + jsonNode);
        }

        String pageID = node.asText();
        if (pageID.isEmpty()) {
            throw new IllegalArgumentException("The page_id field cannot be null or empty in the PAGE_VIEW user event. " + jsonNode);
        }
    }

    /**
     * Validates PURCHASE User Event data.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @throws IllegalArgumentException If the User Event data fails validation.
     */
    public void testPurchase(JsonNode jsonNode) throws IllegalArgumentException {
        JsonNode itemsNode = jsonNode.get("items");
        if (itemsNode == null || !itemsNode.isArray()) {
            throw new IllegalArgumentException("The items field must be a valid array for the PURCHASE event. " + jsonNode);
        }

        ArrayNode items = (ArrayNode) itemsNode;
        if (items.isEmpty()) {
            throw new IllegalArgumentException("The items field must not be empty for the PURCHASE event. " + jsonNode);
        }
    }

    /**
     * Removes JSON nodes with null values from the JSON object.
     *
     * @param jsonNode The User Event data represented as a FasterXML JsonNode instance.
     * @return A new JSON object with null-value fields removed.
     */
    public JsonNode filterNulls(JsonNode jsonNode) {
        // Return early for null
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }
        // Return as-is for non-container nodes        
        if (!jsonNode.isContainerNode()) {
            return jsonNode;
        }

        // Handle array input
        if (jsonNode.isArray()) {
            ArrayNode filteredArray = objectMapper.createArrayNode();
            for (JsonNode element : jsonNode) {
                JsonNode filtered = filterNulls(element);
                if (filtered != null) {
                    filteredArray.add(filtered);
                }
            }
            return (JsonNode) filteredArray;
        }

        // Handle object input
        ObjectNode filteredNode = objectMapper.createObjectNode();
        var fields = jsonNode.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            JsonNode value = field.getValue();
            String fieldName = field.getKey();
            
            // Skip null values immediately
            if (value.isNull()) {
                continue;
            }
            // Handle non-container values first (most common case)
            if (!value.isContainerNode()) {
                filteredNode.set(fieldName, value);
                continue;
            }
            // Handle arrays next
            if (value.isArray()) {
                JsonNode filtered = filterArray(value);
                filteredNode.set(fieldName, filtered);
            }
            // Handle objects last
            JsonNode filtered = filterNulls(value);
            filteredNode.set(fieldName, filtered);
        }

        return (JsonNode) filteredNode;
    }

    /**
     * Removes null-value elements from a JSON array.
     *
     * @param arrayNode The JSON array represented as a FasterXML JsonNode instance.
     * @return A new JSON array with null-value elements removed.
     */
    public JsonNode filterArray(JsonNode arrayNode) {
        if (arrayNode == null) {
            return null;
        }
        ArrayNode filteredArray = objectMapper.createArrayNode();
        for (JsonNode element : arrayNode) {
            // Skip null values immediately
            if (element.isNull()) {
                continue;
            }
            // Handle non-container values first (most common case)
            if (!element.isContainerNode()) {
                filteredArray.add(element);
                continue;
            }
            // Handle arrays next
            if (element.isArray()) {
                JsonNode filtered = filterArray(element);
                filteredArray.add(filtered);
                continue;
            }
            // Handle objects last
            JsonNode filtered = filterNulls(element);
            filteredArray.add(filtered);
        }
        return filteredArray;
    }
}