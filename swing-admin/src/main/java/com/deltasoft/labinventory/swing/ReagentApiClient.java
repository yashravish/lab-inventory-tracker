package com.deltasoft.labinventory.swing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class ReagentApiClient {

    private static final int DEFAULT_PAGE_SIZE = 200;

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String authorizationHeader;

    public ReagentApiClient(String baseUrl) {
        this(baseUrl, null, null);
    }

    public ReagentApiClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (username != null && !username.isBlank() && password != null) {
            String token = Base64.getEncoder()
                    .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            this.authorizationHeader = "Basic " + token;
        } else {
            this.authorizationHeader = null;
        }
    }

    public String authorizationHeader() {
        return authorizationHeader;
    }

    public List<ReagentDto> list(String search) throws Exception {
        return list(search, 0, DEFAULT_PAGE_SIZE);
    }

    public List<ReagentDto> list(String search, int page, int size) throws Exception {
        StringBuilder url = new StringBuilder(baseUrl).append("/api/reagents?")
                .append("page=").append(page)
                .append("&size=").append(size);
        if (search != null && !search.isBlank()) {
            url.append("&search=").append(URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
        }
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(url.toString()))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15));
        if (authorizationHeader != null) {
            reqBuilder.header("Authorization", authorizationHeader);
        }
        HttpResponse<String> res = http.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 401) {
            throw new UnauthorizedException("API returned HTTP 401");
        }
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("API returned HTTP " + res.statusCode() + ": " + res.body());
        }
        return parsePage(res.body());
    }

    /**
     * Accepts either the new paged shape ({"content":[...], ...}) or a bare JSON array,
     * so the viewer keeps working against older backend revisions.
     */
    public List<ReagentDto> parsePage(String json) throws Exception {
        JsonNode root = mapper.readTree(json);
        if (root.isArray()) {
            return parseArray(root);
        }
        JsonNode content = root.get("content");
        if (content != null && content.isArray()) {
            return parseArray(content);
        }
        throw new RuntimeException("Unexpected response shape: " + json);
    }

    /** Retained for backwards compatibility with callers/tests using the old name. */
    public List<ReagentDto> parseList(String json) throws Exception {
        return parsePage(json);
    }

    private List<ReagentDto> parseArray(JsonNode array) throws Exception {
        ReagentDto[] arr = mapper.treeToValue(array, ReagentDto[].class);
        return new ArrayList<>(Arrays.asList(arr));
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String msg) { super(msg); }
    }
}
