package com.moloco.mcm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * The Main class serves as the entry point for testing the UserEventSinkConnector
 * by sending sample user event data in JSON format. It demonstrates how to use the
 * UserEventSinkConnector to process various types of events (e.g., HOME, ITEM_PAGE_VIEW,
 * SEARCH, PAGE_VIEW, PURCHASE).
 */
public class Main {

    /**
     * The main method initializes a UserEventSinkConnector and an ObjectMapper, then creates
     * and sends sample JSON user event data to the connector. Each JSON data string represents
     * a distinct user event type, which the connector processes to ensure proper validation and
     * handling.
     *
     * @param args Command-line arguments, not used in this example
     */
    public static void main(String[] args) {
        String platformId = System.getenv("PLATFORM_ID");
        String apiHostname = System.getenv("API_HOSTNAME"); 
        String apiKey = System.getenv("API_KEY");
        
        // Default to 100 and 10 if env vars not set
        int maxTotalConnections = Integer.parseInt(System.getenv().getOrDefault("MAX_TOTAL_CONNECTIONS", "100"));
        UserEventSinkConnector connector = null;

        try {

            connector = new UserEventSinkConnector(
                platformId,
                apiHostname,
                apiKey,
                maxTotalConnections
            );

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonData = null;

            // Sample events are created and sent to the connector here:
            String[] jsonStrings = new String[]{
                // HOME event
                "{\"id\":\"ajs-next-1729973784295-1f893e86-ce88-430c-9822-5d37ef1c4a22\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"ecd14bdae1469f963df2726f88a2eab5bdd53953\",\"session_id\":\"\",\"name\":\"Dashboard Viewed\",\"event_type\":\"HOME\"}",
                
                // ITEM_PAGE_VIEW event
                "{\"id\":\"ajs-next-1729986778105-2e18cab3-8fd4-473f-8953-d5899fac8c03\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"f5414d1f1c08947254bd257b661aa90b15e092f6\",\"session_id\":\"\",\"event_type\":\"ITEM_PAGE_VIEW\",\"items\":[{\"id\":\"2199832\"}]}",
                // "{\"id\":\"ajs-next-1729986778105-2e18cab3-8fd4-473f-8953-d5899fac8c03\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"f5414d1f1c08947254bd257b661aa90b15e092f6\",\"session_id\":\"\",\"event_type\":\"ITEM_PAGE_VIEW\",\"items\":[]}",
                // "{\"id\":\"ajs-next-1729986778105-2e18cab3-8fd4-473f-8953-d5899fac8c03\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"f5414d1f1c08947254bd257b661aa90b15e092f6\",\"session_id\":\"\",\"event_type\":\"ITEM_PAGE_VIEW\",\"items\":null}",
                // "{\"id\":\"ajs-next-1729986778105-2e18cab3-8fd4-473f-8953-d5899fac8c03\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"f5414d1f1c08947254bd257b661aa90b15e092f6\",\"session_id\":\"\",\"event_type\":\"ITEM_PAGE_VIEW\",\"items\":{\"item_id\":\"1234\"}}",
                
                // SEARCH event
                "{\"id\":\"ajs-next-1729981795685-5b1deab4-39d7-4bfa-af3b-3e183f68c0ae\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"1a0a1ee31dab3661f80a7b621816d53630ff0360\",\"session_id\":\"\",\"event_type\":\"SEARCH\",\"search_query\":\"colorado vape\"}",
                // "{\"id\":\"ajs-next-1729981795685-5b1deab4-39d7-4bfa-af3b-3e183f68c0ae\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"1a0a1ee31dab3661f80a7b621816d53630ff0360\",\"session_id\":\"\",\"event_type\":\"SEARCH\",\"search_query\":null}",
                // "{\"id\":\"ajs-next-1729981795685-5b1deab4-39d7-4bfa-af3b-3e183f68c0ae\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"1a0a1ee31dab3661f80a7b621816d53630ff0360\",\"session_id\":\"\",\"event_type\":\"SEARCH\",\"search_query\":\"\"}",
                // "{\"id\":\"ajs-next-1729981795685-5b1deab4-39d7-4bfa-af3b-3e183f68c0ae\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"1a0a1ee31dab3661f80a7b621816d53630ff0360\",\"session_id\":\"\",\"event_type\":\"SEARCH\"}",
                
                // PAGE_VIEW event
                "{\"id\":\"ajs-next-1729981991771-ef5e378e-6e6f-41d9-add1-6984bc2dda77\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"02db0a1ed22424abc404362bf98c704cb22474ef\",\"session_id\":\"\",\"name\":\"Shop Brands Viewed\",\"event_type\":\"PAGE_VIEW\",\"page_id\":\"SHOP_BRANDS\"}",
                // "{\"id\":\"ajs-next-1729981991771-ef5e378e-6e6f-41d9-add1-6984bc2dda77\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"02db0a1ed22424abc404362bf98c704cb22474ef\",\"session_id\":\"\",\"name\":\"Shop Brands Viewed\",\"event_type\":\"PAGE_VIEW\",\"page_id\":\"\"}",
                // "{\"id\":\"ajs-next-1729981991771-ef5e378e-6e6f-41d9-add1-6984bc2dda77\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"02db0a1ed22424abc404362bf98c704cb22474ef\",\"session_id\":\"\",\"name\":\"Shop Brands Viewed\",\"event_type\":\"PAGE_VIEW\",\"page_id\":null}",
                // "{\"id\":\"ajs-next-1729981991771-ef5e378e-6e6f-41d9-add1-6984bc2dda77\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"02db0a1ed22424abc404362bf98c704cb22474ef\",\"session_id\":\"\",\"name\":\"Shop Brands Viewed\",\"event_type\":\"PAGE_VIEW\"}",
                
                // PURCHASE event
                "{\"id\":\"88ec57bb-bd54-4391-b296-c7de36c45728\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"a9d220010dc9d8f0b998fa45e5f538d4176958da\",\"session_id\":\"\",\"items\":[{\"id\":\"2031646\",\"price\":{\"amount\":50,\"currency\":\"USD\"},\"quantity\":25}],\"revenue\":{\"amount\":1250,\"currency\":\"USD\"},\"event_type\":\"PURCHASE\",\"shipping_charge\":{\"amount\":0,\"currency\":\"USD\"}}"
                // "{\"id\":\"88ec57bb-bd54-4391-b296-c7de36c45728\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"a9d220010dc9d8f0b998fa45e5f538d4176958da\",\"session_id\":\"\",\"items\":[],\"revenue\":{\"amount\":1250,\"currency\":\"USD\"},\"event_type\":\"PURCHASE\",\"shipping_charge\":{\"amount\":0,\"currency\":\"USD\"}}"
                // "{\"id\":\"88ec57bb-bd54-4391-b296-c7de36c45728\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"a9d220010dc9d8f0b998fa45e5f538d4176958da\",\"session_id\":\"\",\"items\":null,\"revenue\":{\"amount\":1250,\"currency\":\"USD\"},\"event_type\":\"PURCHASE\",\"shipping_charge\":{\"amount\":0,\"currency\":\"USD\"}}"
                // "{\"id\":\"88ec57bb-bd54-4391-b296-c7de36c45728\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"a9d220010dc9d8f0b998fa45e5f538d4176958da\",\"session_id\":\"\",\"items\":{},\"revenue\":{\"amount\":1250,\"currency\":\"USD\"},\"event_type\":\"PURCHASE\",\"shipping_charge\":{\"amount\":0,\"currency\":\"USD\"}}"
                // "{\"id\":\"88ec57bb-bd54-4391-b296-c7de36c45728\",\"timestamp\":\"1617870506121\",\"channel_type\":\"SITE\",\"user_id\":\"a9d220010dc9d8f0b998fa45e5f538d4176958da\",\"session_id\":\"\",\"items\":\"string\",\"revenue\":{\"amount\":1250,\"currency\":\"USD\"},\"event_type\":\"PURCHASE\",\"shipping_charge\":{\"amount\":0,\"currency\":\"USD\"}}"
            };

            for (String jsonString : jsonStrings) {
                int maxRetries = 3;
                int retryCount = 0;
                long waitTime = 100; // Start with 0.1 seconds

                while (retryCount < maxRetries) {
                    try {
                        jsonData = mapper.readTree(jsonString);
                        connector.send(jsonData);
                        break; // Success - exit retry loop
                    } catch (IllegalArgumentException e) {
                        // If the JSON is invalid, throw the exception immediately
                        throw e;
                    } catch (IOException e) {
                        // If the other error occurs, retry the event
                        retryCount++;
                        if (retryCount == maxRetries) {
                            System.err.println("Failed after " + maxRetries + " retries for event: " + jsonString);
                            throw e;
                        }
                        System.out.println("Retry " + retryCount + " after " + waitTime + "ms");
                        Thread.sleep(waitTime);
                        waitTime *= 2; // Exponential backoff
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connector != null) {
                connector.close();
            }
        }
    }
}