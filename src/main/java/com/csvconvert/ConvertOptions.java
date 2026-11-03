package com.csvconvert;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public record ConvertOptions(
        List<String> columns,
        Charset inputEncoding,
        Charset outputEncoding,
        char delimiter,
        Character quote,
        int headerRows,
        boolean reverseRows
) {
    public static final ConvertOptions DEFAULT = new ConvertOptions(
            List.of(),
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_8,
            ',',
            '"',
            1,
            false
    );

    public ConvertOptions {
        columns = List.copyOf(Objects.requireNonNull(columns, "columns"));
        Objects.requireNonNull(inputEncoding, "inputEncoding");
        Objects.requireNonNull(outputEncoding, "outputEncoding");
        if (headerRows < 0) {
            throw new CsvConvertException("ヘッダ行数は0以上で指定してください: " + headerRows);
        }
    }

    boolean hasHeaderNames() {
        return headerRows > 0;
    }

    CSVFormat csvFormat() {
        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setRecordSeparator("\n")
                .setIgnoreEmptyLines(true)
                .setTrim(false);
        if (quote == null) {
            builder.setQuote(null).setEscape('\\').setQuoteMode(QuoteMode.NONE);
        } else {
            builder.setQuote(quote);
        }
        return builder.build();
    }
}
