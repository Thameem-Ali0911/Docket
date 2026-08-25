package com.docket.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanitizationUtilsTest {

    @Test
    @DisplayName("stripNulBytes returns null for null input")
    void testNullInput() {
        assertNull(SanitizationUtils.stripNulBytes(null));
    }

    @Test
    @DisplayName("stripNulBytes returns identical string when no NUL bytes present")
    void testNoNulBytes() {
        String input = "Invoice #12345 for Acme Corp Total: $500.00";
        assertEquals(input, SanitizationUtils.stripNulBytes(input));
    }

    @Test
    @DisplayName("stripNulBytes removes embedded and trailing NUL (0x00) characters")
    void testStripsEmbeddedNulBytes() {
        String input = "Total\u0000: $50\u00000.00\u0000";
        String expected = "Total: $500.00";
        assertEquals(expected, SanitizationUtils.stripNulBytes(input));
    }
}
