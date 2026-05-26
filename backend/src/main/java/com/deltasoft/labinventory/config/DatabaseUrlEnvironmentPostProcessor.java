package com.deltasoft.labinventory.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Translates a {@code DATABASE_URL} env var (the format Railway, Heroku, and
 * most managed Postgres providers use) into Spring's
 * {@code spring.datasource.url}, {@code .username}, and {@code .password}.
 *
 * Skips translation when DATABASE_URL is unset or when the user has already set
 * SPRING_DATASOURCE_URL — real env vars win.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "databaseUrlEnv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        // If the operator set SPRING_DATASOURCE_URL explicitly, defer to that.
        String existing = environment.getProperty("SPRING_DATASOURCE_URL");
        if (existing != null && !existing.isBlank()) {
            return;
        }

        Map<String, Object> derived = parse(databaseUrl);
        if (!derived.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, derived));
        }
    }

    private static Map<String, Object> parse(String raw) {
        Map<String, Object> map = new LinkedHashMap<>();
        try {
            // URI can't parse "postgresql://" directly with userinfo because the
            // scheme parser handles it fine. Java's URI accepts "postgres://" too.
            URI uri = new URI(raw);
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String db = path.startsWith("/") ? path.substring(1) : path;
            String userInfo = uri.getUserInfo();
            String user = null;
            String pass = null;
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    user = userInfo.substring(0, colon);
                    pass = userInfo.substring(colon + 1);
                } else {
                    user = userInfo;
                }
            }

            if (host == null || db.isEmpty()) {
                return map;
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;
            String query = uri.getRawQuery();
            if (query != null && !query.isBlank()) {
                jdbcUrl = jdbcUrl + "?" + query;
            }
            map.put("spring.datasource.url", jdbcUrl);
            if (user != null) map.put("spring.datasource.username", user);
            if (pass != null) map.put("spring.datasource.password", pass);
            map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        } catch (URISyntaxException ignored) {
            // Malformed DATABASE_URL — let Spring fail later with a clearer error.
        }
        return map;
    }
}
