package com.example.ucmconnectapi.util;

/**
 * Sanitizes external input before logging to prevent CRLF injection (log forging).
 * Removes carriage return and line feed characters that could be used
 * to inject fake log entries.
 */
public final class LogSanitizer {

    private LogSanitizer() {}

    public static String sanitize(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\r\\n]", "_");
    }
}
