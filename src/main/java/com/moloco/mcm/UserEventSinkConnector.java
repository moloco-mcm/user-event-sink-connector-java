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
 * The main library class for sending user events to Moloco MCM's User Event API endpoint.
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
     * Constructs a new UserEventSinkConnector with the specified parameters.
     *
     * @param builder The builder with configuration parameters.
     */
    private UserEventSinkConnector(Builder builder) {
        this.eventApiHostname = builder.eventApiHostname;
        this.eventApiKey = builder.eventApiKey;
        this.platformID = builder.platformID;
        this.maxTotalConnections = builder.maxTotalConnections;
        this.retryMaxAttempts = builder.retryMaxAttempts;
        this.retryExponentialBackoffMultiplier = builder.retryExponentialBackoffMultiplier;
        this.retryDelayMilliseconds = builder.retryDelayMilliseconds;
        this.utils = new UserEventUtils();
    }

    /**
     * Ensures the HTTP client is initialized for making requests.
     */
    private void ensureHttpClientInitialized() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
                    connectionManager.setMaxTotal(maxTotalConnections);
                    connectionManager.setDefaultMaxPerRoute(maxTotalConnections);
                    httpClient = HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .build();
                }
            }
        }
    }


    /**
     * Sends event data to the specified endpoint.
     *
     * @param jsonString a string representing the event data in JSON format
     * @throws IllegalArgumentException if any argument is invalid, entity is null or if content length &gt; Integer.MAX_VALUE
     * @throws ParseException if header elements cannot be parsed
     * @throws IOException if an error occurs reading the input stream
     * @throws InterruptedException if any thread has interrupted the current thread. The <i>interrupted status</i> of the current thread is cleared when this exception is thrown.
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
     * @throws InterruptedException if any thread has interrupted the current thread. The <i>interrupted status</i> of the current thread is cleared when this exception is thrown.
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

        int retryCount = 0;
        int waitTimeMilliseconds = this.retryDelayMilliseconds;
        Random random = new Random(); // For generating jitter
        while (retryCount < this.retryMaxAttempts) {
            try {
                ensureHttpClientInitialized();
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
     * @throws UnsupportedCharsetException if the named charset is not available in this instance of the Java virtual machine
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

        /**
         * Sets the event API hostname.
         *
         * @param eventApiHostname the hostname of the event API
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if the hostname is null or empty
         */
        public Builder eventApiHostname(String eventApiHostname) throws IllegalArgumentException {
            this.eventApiHostname = sanitizeParameter("eventApiHostname", eventApiHostname);
            return this;
        }

        /**
         * Sets the event API key.
         *
         * @param eventApiKey the API key for authentication
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if the API key is null or empty
         */
        public Builder eventApiKey(String eventApiKey) throws IllegalArgumentException {
            this.eventApiKey = sanitizeParameter("eventApiKey", eventApiKey);
            return this;
        }

        /**
         * Sets the platform ID.
         *
         * @param platformID the platform identifier
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if the platform ID is null or empty
         */
        public Builder platformID(String platformID) throws IllegalArgumentException {
            this.platformID = sanitizeParameter("platformID", platformID);
            return this;
        }

        /**
         * Sets the maximum total connections for the HTTP client.
         *
         * @param maxTotalConnections the maximum total connections to set
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if maxTotalConnections is less than or equal to zero
         */
        public Builder maxTotalConnections(int maxTotalConnections) throws IllegalArgumentException {
            if (maxTotalConnections <= 0) {
                throw new IllegalArgumentException("maxTotalConnections must be at least one(1)");
            }
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param retryMaxAttempts the maximum number of retry attempts
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if retryMaxAttempts is less than 1 or greater than 10
         */
        public Builder retryMaxAttempts(int retryMaxAttempts) throws IllegalArgumentException {
            if (retryMaxAttempts < 1 || 10 < retryMaxAttempts) {
                throw new IllegalArgumentException("retryMaxAttempts must be between 1 and 10");
            }
            this.retryMaxAttempts = retryMaxAttempts;
            return this;
        }

        /**
         * Sets the multiplier for exponential backoff.
         *
         * @param retryExponentialBackoffMultiplier the multiplier for exponential backoff
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if multiplier is less than 1
         */
        public Builder retryExponentialBackoffMultiplier(int retryExponentialBackoffMultiplier) throws IllegalArgumentException {
            if (retryExponentialBackoffMultiplier < 1) {
                throw new IllegalArgumentException("retryExponentialBackoffMultiplier must be at least 1");
            }
            this.retryExponentialBackoffMultiplier = retryExponentialBackoffMultiplier;
            return this;
        }

        /**
         * Sets the delay in milliseconds for the first retry attempt.
         *
         * @param retryDelayMilliseconds the delay in milliseconds
         * @return this Builder instance for method chaining
         * @throws IllegalArgumentException if delayMilliseconds is less than 1
         */
        public Builder retryDelayMilliseconds(int retryDelayMilliseconds) throws IllegalArgumentException {
            if (retryDelayMilliseconds < 1) {
                throw new IllegalArgumentException("retryDelayMilliseconds must be at least one(1)");
            }
            this.retryDelayMilliseconds = retryDelayMilliseconds;
            return this;
        }

        /**
         * Builds a new UserEventSinkConnector instance.
         *
         * @return a new UserEventSinkConnector instance
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public UserEventSinkConnector build() throws IllegalArgumentException {
            eventApiHostname(eventApiHostname);
            eventApiKey(eventApiKey);
            platformID(platformID);
            maxTotalConnections(maxTotalConnections);
            retryMaxAttempts(retryMaxAttempts);
            retryExponentialBackoffMultiplier(retryExponentialBackoffMultiplier);
            retryDelayMilliseconds(retryDelayMilliseconds);
            return new UserEventSinkConnector(this);
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
    }
}