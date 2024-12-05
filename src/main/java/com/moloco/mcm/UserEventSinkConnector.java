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
package com.moloco.mcm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.io.CloseMode;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;
import java.util.Random;

/**
 * The main library class for sending the user events to Moloco MCM's User Event API endpoint.
 */
public class UserEventSinkConnector {

    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 16;
    private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 4;
    private static final int DEFAULT_RETRY_EXPONENTIAL_BACKOFF_MULTIPLIER = 2;
    private static final int DEFAULT_RETRY_DELAY_MILLISECONDS = 100;
    private final String eventApiHostname;
    private final String eventApiKey;
    private final String platformID;
    private final UserEventUtils utils;
    private CloseableHttpClient httpClient;
    private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;
    private int retryMaxAttempts = DEFAULT_RETRY_MAX_ATTEMPTS;
    private int retryExponentialBackoffMultiplier = DEFAULT_RETRY_EXPONENTIAL_BACKOFF_MULTIPLIER;
    private int retryDelayMilliseconds = DEFAULT_RETRY_DELAY_MILLISECONDS;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs a new UserEventSinkConnector with the specified platform ID, API hostname, and key.
     *
     * @param platformID the platform ID (in capital letters and underscore character)
     * @param eventApiHostname the hostname of the user event API
     * @param eventApiKey the User Event API key for authentication
     * @throws IllegalArgumentException if any of the required parameters are null or empty
     */
    public UserEventSinkConnector(
            String platformID,
            String eventApiHostname,
            String eventApiKey)
            throws IllegalArgumentException {
        // Validate constructor parameters
        this.platformID = sanitizeParameter("platformID", platformID);
        this.eventApiHostname = sanitizeParameter("eventApiHostname", eventApiHostname);
        this.eventApiKey = sanitizeParameter("eventApiKey", eventApiKey);
        this.utils = new UserEventUtils();
        this.maxTotalConnections(this.DEFAULT_MAX_TOTAL_CONNECTIONS);
    }

    /**
     * Constructs a new UserEventSinkConnector with the specified parameters.
     *
     * @param builder The builder with configuration parameters.
     */
    private UserEventSinkConnector(Builder builder) {
        this.eventApiHostname = sanitizeParameter("eventApiHostname", builder.eventApiHostname);
        this.eventApiKey = sanitizeParameter("eventApiKey", builder.eventApiKey);
        this.platformID = sanitizeParameter("platformID", builder.platformID);
        this.maxTotalConnections = builder.maxTotalConnections;
        this.retryMaxAttempts = builder.retryMaxAttempts;
        this.retryExponentialBackoffMultiplier = builder.retryExponentialBackoffMultiplier;
        this.retryDelayMilliseconds = builder.retryDelayMilliseconds;
        this.utils = new UserEventUtils();
    }

    public Builder toBuilder() {
        return new Builder()
                .eventApiHostname(this.eventApiHostname)
                .eventApiKey(this.eventApiKey)
                .platformID(this.platformID)
                .maxTotalConnections(this.maxTotalConnections)
                .retryMaxAttempts(this.retryMaxAttempts)
                .retryExponentialBackoffMultiplier(this.retryMaxAttempts)
                .retryDelayMilliseconds(this.retryDelayMilliseconds);
    }
    /**
     * Ensures the HTTP client is initialized.
     */
    private void ensureHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                    connectionManager.setMaxTotal(this.maxTotalConnections);
                    connectionManager.setDefaultMaxPerRoute(this.maxTotalConnections);
                    this.httpClient = HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .build();
                }
            }
        }
    }

    /**
     * Sets the maximum total connections for the HTTP client.
     *
     * @param maxTotalConnections the maximum total connections to set
     * @return this instance for method chaining
     * @throws IllegalArgumentException if maxTotalConnections is less than or equal to zero
     */
    public UserEventSinkConnector maxTotalConnections(int maxTotalConnections) throws IllegalArgumentException {
        if (maxTotalConnections <= 0) {
            throw new IllegalArgumentException("maxTotalConnections should be greater than zero (0)");
        }
        this.maxTotalConnections = maxTotalConnections;

        this.close();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(this.maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(this.maxTotalConnections);
        this.httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        return this;
    }

    /**
     * Validates a parameter is not null or empty and removes CR (\r) and LF (\n) characters.
     *
     * @param paramName the name of the parameter being validated
     * @param value the value to validate
     * @return the validated value
     * @throws IllegalArgumentException if the value is null or empty
     */
    private String sanitizeParameter(String paramName, String value) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if any argument is invalid, entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws ParseException if header elements cannot be parsed
     * @throws IOException if an error occurs reading the input stream
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    public void send(String jsonString)
            throws IllegalArgumentException, ParseException, IOException, InterruptedException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            throw new IllegalArgumentException("The jsonString cannot be null or empty");
        }

        JsonNode jsonNode = objectMapper.readTree(jsonString);
        this.send(jsonNode);
    }

    /**
     * Sends event data to the specified endpoint with jitter added to the exponential backoff.
     *
     * @param jsonNode the event data represented as a FasterXML JSON Node
     * @throws IllegalArgumentException if any argument is invalid, entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws ParseException if header elements cannot be parsed
     * @throws IOException if an error occurs reading the input stream
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    public void send(JsonNode jsonNode)
            throws IllegalArgumentException, ParseException, IOException, InterruptedException {
        utils.validateData(jsonNode);

        String url = String.format("%s/rmp/event/v1/platforms/%s/userevents", eventApiHostname, platformID);
        HttpPost postRequest = new HttpPost(url);
        postRequest.setHeader("Accept", "application/json");
        postRequest.setHeader("Content-Type", "application/json");
        postRequest.setHeader("x-api-key", eventApiKey);
        postRequest.setEntity(new StringEntity(utils.filterNulls(jsonNode).toString(), ContentType.APPLICATION_JSON));

        ensureHttpClient();

        int retryCount = 0;
        int waitTimeMilliseconds = this.retryDelayMilliseconds;
        Random random = new Random(); // For generating jitter
        while (retryCount < this.retryMaxAttempts) {
            try {
                httpClient.execute(postRequest, this::handleResponse);
                return;
            } catch (Exception e) {
                if (retryCount == this.retryMaxAttempts - 1) {
                    throw e;
                }
                // Calculate jitter and add it to the wait time
                int jitter = random.nextInt(100); // Generates a random number between 0 and 99 for jitter
                Thread.sleep(waitTimeMilliseconds + jitter);
                waitTimeMilliseconds *= this.retryExponentialBackoffMultiplier;
                retryCount++;
            }
        }
    }

    /**
     * Processes an HTTP response and returns the content if successful.
     *
     * @param response The HTTP response message
     * @return Content from a successful response
     * @throws ParseException if header elements cannot be parsed
     * @throws IllegalArgumentException if entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws IOException if an error occurs reading the input stream
     * @throws UnsupportedCharsetException Thrown when the named charset is not available in
     * this instance of the Java virtual machine
     */
    private String handleResponse(ClassicHttpResponse response)
            throws ParseException, IllegalArgumentException, IOException, UnsupportedCharsetException {
        Objects.requireNonNull(response, "HTTP response cannot be null");

        int statusCode = response.getCode();
        String responseBody = "";

        if (response.getEntity() != null) {
            responseBody = EntityUtils.toString(response.getEntity());
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
            httpClient = null;
        }
    }

    /**
     * Builder class for UserEventSinkConnector.
     */
    public static class Builder {
        private String eventApiHostname;
        private String eventApiKey;
        private String platformID;
        private int maxTotalConnections = DEFAULT_MAX_TOTAL_CONNECTIONS;
        private int retryMaxAttempts = DEFAULT_RETRY_MAX_ATTEMPTS;
        private int retryExponentialBackoffMultiplier = DEFAULT_RETRY_EXPONENTIAL_BACKOFF_MULTIPLIER;
        private int retryDelayMilliseconds = DEFAULT_RETRY_DELAY_MILLISECONDS;

        public Builder eventApiHostname(String eventApiHostname) {
            this.eventApiHostname = eventApiHostname;
            return this;
        }

        public Builder eventApiKey(String eventApiKey) {
            this.eventApiKey = eventApiKey;
            return this;
        }

        public Builder platformID(String platformID) {
            this.platformID = platformID;
            return this;
        }

        public Builder maxTotalConnections(int maxTotalConnections) {
            if (maxTotalConnections <= 0) {
                throw new IllegalArgumentException("maxTotalConnections should be greater than zero (0)");
            }
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder retryMaxAttempts(int retryMaxAttempts) {
            if (retryMaxAttempts < 1 || retryMaxAttempts > 10) {
                throw new IllegalArgumentException("retryMaxAttempts should be between 1 and 10");
            }
            this.retryMaxAttempts = retryMaxAttempts;
            return this;
        }

        public Builder retryExponentialBackoffMultiplier(int multiplier) {
            if (multiplier < 1) {
                throw new IllegalArgumentException("retryExponentialBackoffMultiplier should be equal to or greater than one(1)");
            }
            this.retryExponentialBackoffMultiplier = multiplier;
            return this;
        }

        public Builder retryDelayMilliseconds(int delayMilliseconds) {
            if (delayMilliseconds < 1) {
                throw new IllegalArgumentException("retryDelayMilliseconds should be equal to or greater than one(1)");
            }
            this.retryDelayMilliseconds = delayMilliseconds;
            return this;
        }

        public UserEventSinkConnector build() {
            return new UserEventSinkConnector(this);
        }
    }
}