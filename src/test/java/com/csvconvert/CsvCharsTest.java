package com.csvconvert;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvCharsTest {
    @Test
    void parsesDelimiterAliases() {
        assertEquals(',', CsvChars.parseDelimiter(","));
        assertEquals(',', CsvChars.parseDelimiter("COMMA"));
        assertEquals('\t', CsvChars.parseDelimiter("TAB"));
        assertEquals('\t', CsvChars.parseDelimiter("\\t"));
        assertEquals(';', CsvChars.parseDelimiter("SEMI"));
        assertEquals('|', CsvChars.parseDelimiter("PIPE"));
        assertEquals(' ', CsvChars.parseDelimiter("SPACE"));
    }

    @Test
    void parsesQuoteAliases() {
        assertEquals('"', CsvChars.parseQuote("\""));
        assertEquals('"', CsvChars.parseQuote("DOUBLE"));
        assertEquals('\'', CsvChars.parseQuote("SINGLE"));
        assertNull(CsvChars.parseQuote("NONE"));
    }

    @Test
    void rejectsInvalidDelimiter() {
        assertThrows(CsvConvertException.class, () -> CsvChars.parseDelimiter("comma-separated"));
    }

    @Test
    void parsesCharset() {
        assertEquals(StandardCharsets.UTF_8, CsvChars.parseCharset("UTF-8"));
        assertThrows(CsvConvertException.class, () -> CsvChars.parseCharset("not-a-charset"));
    }
}
