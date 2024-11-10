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
     * @param maxConnectionsPerRoute the maximum connections per route. Defaults to 10
     * @throws IllegalArgumentException if any of the required parameters are null or empty
     */
    public UserEventSinkConnector(String platformID, String eventApiHostname, String eventApiKey, int maxTotalConnections, int maxConnectionsPerRoute) throws IllegalArgumentException {
        // Validate constructor parameters
        this.platformID = validateParameter("platformID", platformID);
        this.eventApiHostname = validateParameter("eventApiHostname", eventApiHostname);
        this.eventApiKey = validateParameter("eventApiKey", eventApiKey);

        this.utils = new UserEventUtils();
        this.objectMapper = new ObjectMapper();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections <= 0 ? 100 : maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute <= 0 ? 10 : maxConnectionsPerRoute);

        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }

    /**
     * Validates a parameter is not null or empty.
     *
     * @param paramName the name of the parameter being validated
     * @param value the value to validate
     * @return the validated value
     * @throws IllegalArgumentException if the value is null or empty
     */
    private String validateParameter(String paramName, String value) throws IllegalArgumentException  {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }
        return value.trim();
    }

    /**
     * Sends event data to the specified endpoint.
     *
     * @param jsonString a string representing the event data in JSON format
     * @throws IllegalArgumentException if the input string is null or empty
     * @throws Exception if an error occurs during parsing the input string or sending the data
     */
    public void send(String jsonString) throws Exception {
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
     * @throws Exception if an error occurs during sending
     */
    public void send(JsonNode jsonNode) throws Exception {
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
     * @throws IOException If there's an issue with network communication
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

        if (statusCode >= 200 && statusCode < 300) {
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