package com.csvconvert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parameters used to generate a Windows batch launcher for the CLI.
 * Input/output paths are not stored here; the generated bat takes the input
 * as {@code %1} and writes {@code <prefix><name><suffix>.<ext>}.
 */
public record LaunchConfig(
        String jarPath,
        String columns,
        int headerRows,
        String encoding,
        String inputEncoding,
        String outputEncoding,
        String delimiter,
        String quote,
        String outputPrefix,
        String outputSuffix,
        boolean reverseRows,
        boolean pauseAtEnd
) {
    private static final Pattern UNSAFE_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|&^%!\\r\\n\\t]");

    public LaunchConfig {
        jarPath = BatchScriptBuilder.toBatRelativeJar(jarPath);
        columns = nullToEmpty(columns);
        encoding = blankToDefault(encoding, "UTF-8");
        inputEncoding = nullToEmpty(inputEncoding);
        outputEncoding = nullToEmpty(outputEncoding);
        delimiter = keepDelimiter(delimiter);
        quote = keepQuote(quote);
        outputPrefix = sanitizeNamePart(outputPrefix);
        outputSuffix = sanitizeNamePart(outputSuffix);
        if (outputPrefix.isEmpty() && outputSuffix.isEmpty()) {
            outputSuffix = "_conv";
        }
        if (headerRows < 0) {
            throw new CsvConvertException("ヘッダ行数は0以上で指定してください: " + headerRows);
        }
    }

    public static LaunchConfig defaults() {
        return new LaunchConfig(
                "csv-convert-1.0.0.jar",
                "",
                1,
                "UTF-8",
                "",
                "",
                ",",
                "\"",
                "",
                "_conv",
                false,
                true
        );
    }

    /** Example: {@code data.csv} with prefix {@code pre_} and suffix {@code _out} → {@code pre_data_out.csv}. */
    public String outputNameExample(String sampleBaseName, String sampleExtension) {
        return outputPrefix + sampleBaseName + outputSuffix + sampleExtension;
    }

    /** CLI options only (no input/output positional arguments). */
    public List<String> toCliArgs() {
        List<String> args = new ArrayList<>();
        String trimmedColumns = columns.strip();
        if (!trimmedColumns.isEmpty()) {
            args.add("-c");
            args.add(trimmedColumns);
        }
        if (headerRows != 1) {
            args.add("-H");
            args.add(Integer.toString(headerRows));
        }

        boolean splitEncoding = !inputEncoding.isBlank() || !outputEncoding.isBlank();
        if (splitEncoding) {
            String inEnc = inputEncoding.isBlank() ? encoding : inputEncoding.strip();
            String outEnc = outputEncoding.isBlank() ? encoding : outputEncoding.strip();
            args.add("--input-encoding");
            args.add(inEnc);
            args.add("--output-encoding");
            args.add(outEnc);
        } else if (!"UTF-8".equalsIgnoreCase(encoding.strip())) {
            args.add("-e");
            args.add(encoding.strip());
        }

        String delimiterToken = normalizeDelimiter(delimiter);
        if (!",".equals(delimiterToken) && !"COMMA".equalsIgnoreCase(delimiterToken)) {
            args.add("-d");
            args.add(delimiterToken);
        }

        String quoteToken = normalizeQuote(quote);
        if (!"\"".equals(quoteToken) && !"DOUBLE".equalsIgnoreCase(quoteToken)) {
            args.add("-q");
            args.add(quoteToken);
        }

        if (reverseRows) {
            args.add("--reverse");
        }
        return List.copyOf(args);
    }

    static String normalizeDelimiter(String value) {
        String token = Objects.requireNonNull(value);
        if (token.length() != 1) {
            token = token.strip();
        }
        return switch (token) {
            case "," -> "COMMA";
            case "\t", "\\t" -> "TAB";
            case ";" -> "SEMI";
            case "|" -> "PIPE";
            case " " -> "SPACE";
            default -> token;
        };
    }

    static String normalizeQuote(String value) {
        String token = Objects.requireNonNull(value);
        if (token.length() != 1) {
            token = token.strip();
        }
        return switch (token) {
            case "\"" -> "DOUBLE";
            case "'" -> "SINGLE";
            case "" -> "NONE";
            default -> token;
        };
    }

    static String sanitizeNamePart(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String cleaned = UNSAFE_NAME_CHARS.matcher(value).replaceAll("");
        if (!cleaned.equals(value)) {
            throw new CsvConvertException(
                    "出力プレフィックス/サフィックスに使えません: \\ / : * ? \" < > | & ^ % ! および改行・タブ");
        }
        return value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String blankToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.strip();
    }

    /** Keeps whitespace delimiters such as TAB/SPACE; only trims multi-character aliases. */
    private static String keepDelimiter(String value) {
        if (value == null || value.isEmpty()) {
            return ",";
        }
        if (value.length() == 1) {
            return value;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? "," : stripped;
    }

    private static String keepQuote(String value) {
        if (value == null || value.isEmpty()) {
            return "\"";
        }
        if (value.length() == 1) {
            return value;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? "\"" : stripped;
    }
}
