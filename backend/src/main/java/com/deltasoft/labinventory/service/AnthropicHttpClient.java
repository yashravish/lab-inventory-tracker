package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.web.dto.AiQueryFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

class AnthropicHttpClient {

    static final String API_URL = "https://api.anthropic.com/v1/messages";
    static final String ANTHROPIC_VERSION = "2023-06-01";

    static final String SYSTEM_PROMPT = """
            You are the query parser for a lab inventory app. Convert the user's
            request into exactly one call to the query_inventory tool. Never reply
            with plain text — always call the tool.

            Schema fields you can fill:
              search             substring matched against name, supplier, storageLocation
              status             IN_STOCK | LOW_STOCK | EXPIRED
              hazardClass        FLAMMABLE | CORROSIVE | TOXIC | NONE
              expiresWithinDays  integer >= 0; reagents whose expirationDate is between
                                 today and today + N days inclusive
              sort               one of: name,asc | name,desc | supplier,asc |
                                 supplier,desc | quantity,asc | quantity,desc |
                                 expirationDate,asc | expirationDate,desc
              size               1..200
              interpretation     one short sentence summarizing the filter

            Mapping rules:
              "flammables" / "flammable" -> hazardClass=FLAMMABLE
              "acids" / "corrosives" / "corrosive" -> hazardClass=CORROSIVE
              "toxic" / "poison" -> hazardClass=TOXIC
              "expiring soon" / "about to expire" / "expires soon" without a number
                -> expiresWithinDays=30
              "expires within N days/weeks/months" -> expiresWithinDays=N (convert
                weeks*7, months*30; cap at 365)
              "expired" / "past expiration" / "old" -> status=EXPIRED
              "low stock" / "below minimum" / "running low" / "running out"
                -> status=LOW_STOCK
              "in stock" / "healthy" -> status=IN_STOCK
              A named supplier, lab, shelf, or chemical -> goes into search.
              If the user asks to sort, set sort. Otherwise leave sort empty.
              Status precedence: EXPIRED beats LOW_STOCK beats IN_STOCK. If the user
              asks for "expired flammables", emit status=EXPIRED and
              hazardClass=FLAMMABLE.

            If the request is ambiguous or unrelated to inventory, still call the
            tool, leave fields empty, and put a short clarifying sentence in
            interpretation.
            """;

    static final String TOOL_NAME = "query_inventory";

    private static final List<String> ALLOWED_STATUS = List.of("IN_STOCK", "LOW_STOCK", "EXPIRED");
    private static final List<String> ALLOWED_HAZARD = List.of("FLAMMABLE", "CORROSIVE", "TOXIC", "NONE");
    private static final List<String> ALLOWED_SORT = List.of(
            "name,asc", "name,desc", "supplier,asc", "supplier,desc",
            "quantity,asc", "quantity,desc", "expirationDate,asc", "expirationDate,desc");

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http;
    private final String apiKey;
    private final String model;

    AnthropicHttpClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    NlParseResult parse(String userQuery) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", 256);

        // system + tools both carry cache_control: ephemeral so the prompt prefix is
        // cached across requests. Only the user message varies turn-to-turn.
        ArrayNode systemBlocks = mapper.createArrayNode();
        ObjectNode sysBlock = systemBlocks.addObject();
        sysBlock.put("type", "text");
        sysBlock.put("text", SYSTEM_PROMPT);
        sysBlock.putObject("cache_control").put("type", "ephemeral");
        root.set("system", systemBlocks);

        ArrayNode tools = mapper.createArrayNode();
        ObjectNode tool = tools.addObject();
        tool.put("name", TOOL_NAME);
        tool.put("description",
                "Returns the structured filter that best matches the user's request. "
                        + "Always emit at least the `interpretation` field.");
        ObjectNode inputSchema = tool.putObject("input_schema");
        inputSchema.put("type", "object");
        ObjectNode props = inputSchema.putObject("properties");
        props.putObject("search").put("type", "string").put("description",
                "Free-text substring; matched against name, supplier, storageLocation.");
        ObjectNode status = props.putObject("status");
        status.put("type", "string");
        ArrayNode statusEnum = status.putArray("enum");
        ALLOWED_STATUS.forEach(statusEnum::add);
        ObjectNode hazard = props.putObject("hazardClass");
        hazard.put("type", "string");
        ArrayNode hazardEnum = hazard.putArray("enum");
        ALLOWED_HAZARD.forEach(hazardEnum::add);
        ObjectNode ewd = props.putObject("expiresWithinDays");
        ewd.put("type", "integer");
        ewd.put("minimum", 0);
        ewd.put("maximum", 365);
        ObjectNode sort = props.putObject("sort");
        sort.put("type", "string");
        ArrayNode sortEnum = sort.putArray("enum");
        ALLOWED_SORT.forEach(sortEnum::add);
        ObjectNode size = props.putObject("size");
        size.put("type", "integer");
        size.put("minimum", 1);
        size.put("maximum", 200);
        props.putObject("interpretation").put("type", "string").put("description",
                "One short sentence summarizing the filter.");
        inputSchema.putArray("required").add("interpretation");
        tool.putObject("cache_control").put("type", "ephemeral");
        root.set("tools", tools);

        ObjectNode toolChoice = root.putObject("tool_choice");
        toolChoice.put("type", "tool");
        toolChoice.put("name", TOOL_NAME);

        ArrayNode messages = root.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", userQuery);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(20))
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new RuntimeException("Anthropic API " + res.statusCode() + ": " + res.body());
        }
        JsonNode body = mapper.readTree(res.body());
        JsonNode content = body.get("content");
        if (content == null || !content.isArray()) {
            throw new RuntimeException("Anthropic response missing content");
        }
        for (JsonNode block : content) {
            if ("tool_use".equals(block.path("type").asText())
                    && TOOL_NAME.equals(block.path("name").asText())) {
                JsonNode input = block.get("input");
                return fromToolInput(input);
            }
        }
        throw new RuntimeException("Anthropic response missing tool_use block");
    }

    private NlParseResult fromToolInput(JsonNode input) {
        AiQueryFilter f = new AiQueryFilter();
        if (input == null) return new NlParseResult(f, "Showing all reagents.");
        f.setSearch(textOrNull(input, "search"));
        f.setStatus(enumOrNull(input, "status", ALLOWED_STATUS));
        f.setHazardClass(enumOrNull(input, "hazardClass", ALLOWED_HAZARD));
        if (input.hasNonNull("expiresWithinDays") && input.get("expiresWithinDays").isInt()) {
            int days = input.get("expiresWithinDays").asInt();
            if (days >= 0) f.setExpiresWithinDays(Math.min(days, 365));
        }
        f.setSort(enumOrNull(input, "sort", ALLOWED_SORT));
        if (input.hasNonNull("size") && input.get("size").isInt()) {
            int s = input.get("size").asInt();
            f.setSize(Math.max(1, Math.min(s, 200)));
        }
        String interpretation = textOrNull(input, "interpretation");
        if (interpretation == null || interpretation.isBlank()) {
            interpretation = "Showing matching reagents.";
        }
        return new NlParseResult(f, interpretation);
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return s.isBlank() ? null : s;
    }

    private static String enumOrNull(JsonNode n, String field, List<String> allowed) {
        String s = textOrNull(n, field);
        if (s == null) return null;
        return allowed.contains(s) ? s : null;
    }
}
