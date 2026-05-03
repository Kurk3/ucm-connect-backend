package com.example.ucmconnectapi.util;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Sanitizes user-provided HTML content to prevent XSS attacks.
 * Strips all dangerous HTML elements (script, onclick, onerror, etc.)
 * while preserving safe text content.
 */
public final class HtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS);

    private HtmlSanitizer() {}

    public static String sanitize(String input) {
        if (input == null) return null;
        return POLICY.sanitize(input);
    }
}
