package com.example.aikb.common;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility methods for logging AI and request data without leaking secrets or full business text.
 */
public final class LogSanitizer {

    public static final int DEFAULT_PREVIEW_LENGTH = 120;
    public static final int DEFAULT_ERROR_LENGTH = 200;
    public static final int DEFAULT_VECTOR_ITEMS = 5;
    private static final String MASKED = "****";

    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern SECRET_ASSIGNMENT_PATTERN = Pattern.compile(
            "(?i)(authorization|api[_-]?key|token|password|passwordHash|salt)(\\s*[:=]\\s*)([^\\s,;}&]+)");
    private static final Pattern JSON_SECRET_PATTERN = Pattern.compile(
            "(?i)(\"(?:authorization|api[_-]?key|token|password|passwordHash|salt)\"\\s*:\\s*\")([^\"]*)(\")");

    private LogSanitizer() {
    }

    public static String preview(String text) {
        return preview(text, DEFAULT_PREVIEW_LENGTH);
    }

    public static String preview(String text, int maxLength) {
        if (text == null) {
            return "<null>";
        }
        int safeMaxLength = Math.max(0, maxLength);
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= safeMaxLength) {
            return normalized;
        }
        return normalized.substring(0, safeMaxLength) + "...";
    }

    public static String maskSecret(String value) {
        return value == null ? null : MASKED;
    }

    public static String safeErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "<null>";
        }
        return throwable.getClass().getSimpleName() + ": " + safeMessage(throwable.getMessage());
    }

    public static String safeMessage(String message) {
        return sanitize(message, DEFAULT_ERROR_LENGTH);
    }

    public static String sanitize(String value, int maxLength) {
        if (value == null) {
            return "<null>";
        }
        String sanitized = BEARER_PATTERN.matcher(value).replaceAll("Bearer " + MASKED);
        sanitized = SECRET_ASSIGNMENT_PATTERN.matcher(sanitized).replaceAll("$1$2" + MASKED);
        sanitized = JSON_SECRET_PATTERN.matcher(sanitized).replaceAll("$1" + MASKED + "$3");
        return preview(sanitized, maxLength);
    }

    public static String vectorPreview(List<? extends Number> vector) {
        return vectorPreview(vector, DEFAULT_VECTOR_ITEMS);
    }

    public static String vectorPreview(List<? extends Number> vector, int maxItems) {
        if (vector == null) {
            return "<null>";
        }
        int limit = Math.max(0, maxItems);
        StringBuilder builder = new StringBuilder("[");
        int itemsToShow = Math.min(limit, Math.max(0, vector.size() - 1));
        for (int i = 0; i < itemsToShow; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            Number value = vector.get(i);
            builder.append(value == null ? "null" : String.format(java.util.Locale.ROOT, "%.6f", value.doubleValue()));
        }
        if (!vector.isEmpty()) {
            if (itemsToShow > 0) {
                builder.append(", ");
            }
            builder.append("...");
        }
        builder.append("] size=").append(vector.size());
        return builder.toString();
    }
}
