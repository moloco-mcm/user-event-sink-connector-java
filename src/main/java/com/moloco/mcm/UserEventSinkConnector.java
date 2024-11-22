package com.moloco.mcm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.io.CloseMode;

import java.io.IOException;
import java.util.Objects;

/**
 * The main library class for sending the user events to Moloco MCM's User Event API endpoint.
 */
public class UserEventSinkConnector {

    private final String eventApiHostname;
    private final String eventApiKey;
    private final String platformID;
    private final UserEventUtils utils;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new UserEventSinkConnector with the specified platform ID, API hostname, and key.
     *
     * @param platformID the platform ID (in capital letters and underscore character)
     * @param eventApiHostname the hostname of the user event API
     * @param eventApiKey the User Event API key for authentication
     * @param maxTotalConnections the maximum total number of connections. Defaults to 100
     * @throws IllegalArgumentException if any of the required parameters are null or empty
     */
    public UserEventSinkConnector(
        String platformID, 
        String eventApiHostname, 
        String eventApiKey, 
        int maxTotalConnections) 
        throws IllegalArgumentException {
        // Validate constructor parameters
        this.platformID = sanitizeParameter("platformID", platformID);
        this.eventApiHostname = sanitizeParameter("eventApiHostname", eventApiHostname);
        this.eventApiKey = sanitizeParameter("eventApiKey", eventApiKey);

        this.utils = new UserEventUtils();
        this.objectMapper = new ObjectMapper();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections <= 0 ? 100 : maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxTotalConnections <= 0 ? 100 : maxTotalConnections);

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    /**
     * Validates a parameter is not null or empty and removes CR (\r) and LF (\n) characters.
     *
     * @param paramName the name of the parameter being validated
     * @param value the value to validate
     * @return the validated value
     * @throws IllegalArgumentException if the value is null or empty
     */
    private String sanitizeParameter(String paramName, String value) throws IllegalArgumentException  {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
        // Remove CR (\r) and LF (\n) characters
        return value.trim().replaceAll("[\\r\\n]", "");
    }

    /**
     * Sends event data to the specified endpoint.
     *
     * @param jsonString a string representing the event data in JSON format
     * @throws IllegalArgumentException if the input string is null or empty
     * @throws IOException if an error occurs during sending or receiving the response
     */
    public void send(String jsonString) throws IllegalArgumentException, IOException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("The jsonString cannot be null or empty");
        }

        JsonNode jsonNode = objectMapper.readTree(jsonString);
        this.send(jsonNode);
    }

    /**
     * Sends event data to the specified endpoint.
     *
     * @param jsonNode the event data represented as a FasterXML JSON Node
     * @throws IllegalArgumentException if the input node is null
     * @throws IOException if an error occurs during sending or receiving the response  
     */
    public void send(JsonNode jsonNode) throws IllegalArgumentException, IOException {
        if (jsonNode == null) {
            throw new IllegalArgumentException("The jsonNode cannot be null");
        }

        utils.validateData(jsonNode);

        JsonNode filteredJson = utils.filterNulls(jsonNode);
        if (filteredJson == null) {
            throw new IllegalArgumentException("Failed to process JSON data: filtered result is null");
        }

        String url = String.format("%s/rmp/event/v1/platforms/%s/userevents",
                eventApiHostname, platformID);

        HttpPost postRequest = new HttpPost(url);
        postRequest.setHeader("Accept", "application/json");
        postRequest.setHeader("Content-Type", "application/json");
        postRequest.setHeader("x-api-key", eventApiKey);

        StringEntity entity = new StringEntity(
                filteredJson.toString(),
                ContentType.APPLICATION_JSON
        );
        postRequest.setEntity(entity);
        httpClient.execute(postRequest, this::handleResponse);
    }

    /**
     * Processes an HTTP response and returns the content if successful.
     *
     * @param response The HTTP response message
     * @return Content from a successful response
     * @throws IOException If there's an issue with network communication or the HTTP response
     */
    private String handleResponse(ClassicHttpResponse response) throws IOException {
        Objects.requireNonNull(response, "HTTP response cannot be null");

        int statusCode = response.getCode();
        String responseBody = "";

        try {
            if (response.getEntity() != null) {
                responseBody = EntityUtils.toString(response.getEntity());
            }
        } catch (IOException | ParseException e) {
            throw new IOException("Failed to read response body: " + e.getMessage(), e);
        }

        if (200 <= statusCode && statusCode < 300) {
            return responseBody;
        } else {
            throw new IOException(String.format("Request failed: status code: %s, reason phrase: %s", statusCode, responseBody));
        }
    }

    /**
     * Releases and frees up the HTTP connection resource.
     */
    public void close() {
        if (httpClient != null) {
            httpClient.close(CloseMode.GRACEFUL);
        }
    }
}