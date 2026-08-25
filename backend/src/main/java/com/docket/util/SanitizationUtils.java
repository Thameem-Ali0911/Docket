package com.docket.util;

/**
 * Shared sanitization utilities across the Docket processing pipeline.
 */
public final class SanitizationUtils {

    private SanitizationUtils() {
        // Utility class
    }

    /**
     * PostgreSQL's text/UTF-8 columns reject literal NUL bytes (0x00) outright under any encoding.
     * Some PDFs (especially with custom TrueType embedded fonts or CMap encodings) yield extracted
     * text containing NUL characters. Stripping NUL bytes prevents DataIntegrityViolationException
     * across all persistence layers (documents, extractions, summaries, and anomalies).
     *
     * @param text the raw string to sanitize
     * @return the string stripped of 0x00 bytes, or null if input was null
     */
    public static String stripNulBytes(String text) {
        if (text == null) {
            return null;
        }
        return text.indexOf('\u0000') == -1 ? text : text.replace("\u0000", "");
    }
}
