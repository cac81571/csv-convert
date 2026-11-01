package com.csvconvert;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;

final class CsvChars {
    private CsvChars() {
    }

    static char parseDelimiter(String value) {
        if (value == null || value.isEmpty()) {
            throw new CsvConvertException("区切り文字が空です。");
        }
        String token = value.strip();
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "TAB", "\\T" -> '\t';
            case "COMMA" -> ',';
            case "SEMI", "SEMICOLON" -> ';';
            case "PIPE" -> '|';
            case "SP", "SPACE" -> ' ';
            default -> singleChar(token, "区切り文字");
        };
    }

    static Character parseQuote(String value) {
        if (value == null || value.isEmpty()) {
            throw new CsvConvertException("囲み文字が空です。NONE を指定すると囲みなしになります。");
        }
        String token = value.strip();
        return switch (token.toUpperCase(Locale.ROOT)) {
            case "NONE" -> null;
            case "DOUBLE" -> '"';
            case "SINGLE" -> '\'';
            default -> singleChar(token, "囲み文字");
        };
    }

    static Charset parseCharset(String value) {
        if (value == null || value.isBlank()) {
            throw new CsvConvertException("文字コードが空です。");
        }
        try {
            return Charset.forName(value.strip());
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
            throw new CsvConvertException("未対応の文字コードです: " + value, ex);
        }
    }

    private static char singleChar(String token, String label) {
        if ("\\t".equals(token)) {
            return '\t';
        }
        if (token.length() == 1) {
            return token.charAt(0);
        }
        throw new CsvConvertException(label + "は1文字、または TAB / COMMA / SEMI / PIPE / SPACE / DOUBLE / SINGLE / NONE で指定してください: " + token);
    }
}
