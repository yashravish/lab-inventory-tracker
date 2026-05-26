package com.deltasoft.labinventory.service;

import com.deltasoft.labinventory.web.dto.AiQueryFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MockNlParser {

    private static final Pattern EXPIRES_PATTERN = Pattern.compile(
            "expir(?:e|es|ing)?\\s+(?:within|in|in\\s+the\\s+next)?\\s*(\\d+)\\s*(day|week|month)s?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LAB_PATTERN = Pattern.compile(
            "(?:in\\s+)?(Lab-\\d+|Shelf-[A-Za-z0-9]+|Cab-\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]+)\"|'([^']+)'");

    private MockNlParser() {}

    public static NlParseResult parse(String raw) {
        AiQueryFilter f = new AiQueryFilter();
        if (raw == null || raw.isBlank()) {
            return new NlParseResult(f, "Showing all reagents.");
        }
        String q = raw.trim();
        String lower = q.toLowerCase();
        List<String> parts = new ArrayList<>();

        if (lower.contains("expired") || lower.contains("past expiration") || lower.contains("old stock")) {
            f.setStatus("EXPIRED");
            parts.add("expired items");
        } else if (lower.contains("low stock") || lower.contains("below minimum")
                || lower.contains("running low") || lower.contains("running out")) {
            f.setStatus("LOW_STOCK");
            parts.add("low-stock items");
        } else if (lower.contains("in stock") || lower.contains("healthy")) {
            f.setStatus("IN_STOCK");
            parts.add("in-stock items");
        }

        if (lower.contains("flammable")) {
            f.setHazardClass("FLAMMABLE");
            parts.add("flammables");
        } else if (lower.contains("corrosive") || lower.contains("acid")) {
            f.setHazardClass("CORROSIVE");
            parts.add("corrosives");
        } else if (lower.contains("toxic") || lower.contains("poison")) {
            f.setHazardClass("TOXIC");
            parts.add("toxic items");
        }

        Matcher em = EXPIRES_PATTERN.matcher(lower);
        if (em.find()) {
            int n = Integer.parseInt(em.group(1));
            String unit = em.group(2).toLowerCase();
            int days = switch (unit) {
                case "week" -> n * 7;
                case "month" -> n * 30;
                default -> n;
            };
            if (days > 365) days = 365;
            f.setExpiresWithinDays(days);
            parts.add("expiring within " + days + " days");
        } else if (lower.contains("expiring soon") || lower.contains("about to expire")
                || lower.contains("expires soon")) {
            f.setExpiresWithinDays(30);
            parts.add("expiring within 30 days");
        }

        Matcher qm = QUOTED_PATTERN.matcher(q);
        if (qm.find()) {
            String quoted = qm.group(1) != null ? qm.group(1) : qm.group(2);
            f.setSearch(quoted);
            parts.add("matching \"" + quoted + "\"");
        } else {
            Matcher lm = LAB_PATTERN.matcher(q);
            if (lm.find()) {
                String loc = lm.group(1);
                f.setSearch(loc);
                parts.add("in " + loc);
            }
        }

        String interpretation;
        if (parts.isEmpty()) {
            interpretation = "Showing all reagents.";
        } else {
            String joined = String.join(", ", parts);
            interpretation = Character.toUpperCase(joined.charAt(0)) + joined.substring(1) + ".";
        }
        return new NlParseResult(f, interpretation);
    }
}
