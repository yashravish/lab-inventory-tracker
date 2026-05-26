package com.deltasoft.labinventory.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenvFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Look for .env relative to the current working dir, the project root, and the parent dir.
        // Whichever exists first wins. Real env vars always take precedence (we don't overwrite them).
        List<Path> candidates = List.of(
                Path.of(".env"),
                Path.of("backend", ".env"),
                Path.of("..", ".env")
        );
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                Map<String, Object> parsed = parse(p);
                if (!parsed.isEmpty()) {
                    environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, parsed));
                }
                return;
            }
        }
    }

    private static Map<String, Object> parse(Path file) {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            for (String raw : Files.readAllLines(file)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.length() >= 2
                        && ((value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                            || (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\''))) {
                    value = value.substring(1, value.length() - 1);
                }
                map.put(key, value);
            }
        } catch (IOException ignored) {
            // Best-effort: if the file can't be read, silently skip. The app still boots in mock mode.
        }
        return map;
    }
}
