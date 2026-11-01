package com.csvconvert;

import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class ColumnSelection {
    private static final Pattern INDEX = Pattern.compile("#?\\d+");

    private final List<Integer> indexes;

    private ColumnSelection(List<Integer> indexes) {
        this.indexes = List.copyOf(indexes);
    }

    static ColumnSelection resolve(List<CSVRecord> headerRows, List<CSVRecord> dataRows, ConvertOptions options) {
        List<String> headerNames = headerNames(headerRows, options);
        int width = columnCount(headerRows, dataRows);
        List<String> requested = options.columns();

        if (requested.isEmpty()) {
            List<Integer> indexes = new ArrayList<>(width);
            for (int i = 0; i < width; i++) {
                indexes.add(i);
            }
            return new ColumnSelection(indexes);
        }

        List<Integer> indexes = new ArrayList<>(requested.size());
        for (String column : requested) {
            indexes.add(resolveIndex(column, headerNames, options.hasHeaderNames(), width));
        }
        return new ColumnSelection(indexes);
    }

    List<String> extract(CSVRecord row) {
        List<String> values = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            values.add(index < row.size() ? row.get(index) : "");
        }
        return values;
    }

    private static List<String> headerNames(List<CSVRecord> headerRows, ConvertOptions options) {
        if (!options.hasHeaderNames() || headerRows.isEmpty()) {
            return List.of();
        }
        return valuesOf(headerRows.get(headerRows.size() - 1));
    }

    private static List<String> valuesOf(CSVRecord record) {
        List<String> values = new ArrayList<>(record.size());
        for (int i = 0; i < record.size(); i++) {
            values.add(record.get(i));
        }
        return values;
    }

    private static int columnCount(List<CSVRecord> headerRows, List<CSVRecord> dataRows) {
        return Stream.concat(headerRows.stream(), dataRows.stream())
                .mapToInt(CSVRecord::size)
                .max()
                .orElse(0);
    }

    private static int resolveIndex(String column, List<String> headerNames, boolean hasHeaderNames, int width) {
        String token = column.strip();
        if (token.isEmpty()) {
            throw new CsvConvertException("空のカラム指定があります。");
        }
        if (INDEX.matcher(token).matches()) {
            int oneBased = Integer.parseInt(token.startsWith("#") ? token.substring(1) : token);
            if (oneBased < 1) {
                throw new CsvConvertException("カラム番号は1以上で指定してください: " + token);
            }
            int index = oneBased - 1;
            if (index >= width) {
                throw new CsvConvertException("カラム番号が範囲外です: " + token + "（列数: " + width + "）");
            }
            return index;
        }
        if (!hasHeaderNames) {
            throw new CsvConvertException("ヘッダ行数が0の場合はカラム名ではなく番号で指定してください: " + token);
        }
        int index = headerNames.indexOf(token);
        if (index < 0) {
            throw new CsvConvertException("対象カラムが見つかりません: " + token + "（ヘッダ: " + String.join(", ", headerNames) + "）");
        }
        return index;
    }
}
