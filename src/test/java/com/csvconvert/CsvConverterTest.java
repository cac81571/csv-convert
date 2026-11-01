package com.csvconvert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvConverterTest {
    private static final String PEOPLE = """
            id,name,age,city
            1,Alice,30,Tokyo
            2,Bob,25,Osaka
            3,Carol,40,Fukuoka
            """;

    @Test
    void extractsColumnsAndReversesRowsKeepingHeader() {
        String actual = convert(PEOPLE, new ConvertOptions(
                List.of("name", "city"),
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                ',',
                '"',
                1
        ));

        assertEquals("""
                name,city
                Carol,Fukuoka
                Bob,Osaka
                Alice,Tokyo
                """, actual);
    }

    @Test
    void extractsByOneBasedIndexes() {
        String actual = convert(PEOPLE, options(List.of("2", "#4")));
        assertEquals("""
                name,city
                Carol,Fukuoka
                Bob,Osaka
                Alice,Tokyo
                """, actual);
    }

    @Test
    void reversesAllColumnsWhenNoneSpecified() {
        String actual = convert(PEOPLE, options(List.of()));
        assertEquals("""
                id,name,age,city
                3,Carol,40,Fukuoka
                2,Bob,25,Osaka
                1,Alice,30,Tokyo
                """, actual);
    }

    @Test
    void keepsQuotedFieldsAndCustomDelimiter() {
        String input = """
                name;note
                Alice;"a;b"
                Bob;"x,y"
                """;
        String actual = convert(input, new ConvertOptions(
                List.of("note", "name"),
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                ';',
                '"',
                1
        ));
        assertEquals("""
                note;name
                x,y;Bob
                "a;b";Alice
                """, actual);
    }

    @Test
    void readsAndWritesShiftJis() {
        Charset shiftJis = Charset.forName("MS932");
        String input = """
                名前,都市
                佐藤,東京
                鈴木,大阪
                """;
        ConvertOptions options = new ConvertOptions(
                List.of("名前", "都市"),
                shiftJis,
                shiftJis,
                ',',
                '"',
                1
        );
        byte[] output = convertBytes(input.getBytes(shiftJis), options);
        assertEquals("""
                名前,都市
                鈴木,大阪
                佐藤,東京
                """, new String(output, shiftJis).replace("\r\n", "\n"));
    }

    @Test
    void skipsUtf8Bom() {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = PEOPLE.getBytes(StandardCharsets.UTF_8);
        byte[] input = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, input, 0, bom.length);
        System.arraycopy(body, 0, input, bom.length, body.length);

        String actual = new String(convertBytes(input, options(List.of("name"))), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        assertEquals("""
                name
                Carol
                Bob
                Alice
                """, actual);
    }

    @Test
    void reversesRowsWithoutHeader() {
        String input = """
                1,Alice
                2,Bob
                3,Carol
                """;
        String actual = convert(input, new ConvertOptions(
                List.of("2"),
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                ',',
                '"',
                0
        ));
        assertEquals("""
                Carol
                Bob
                Alice
                """, actual);
    }

    @Test
    void usesSingleQuoteAndTabDelimiter() {
        String input = "id\tname\n1\tKen\n2\tAnn\n";
        String actual = convert(input, new ConvertOptions(
                List.of("name"),
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                '\t',
                '\'',
                1
        ));
        assertEquals("""
                name
                Ann
                Ken
                """, actual);
    }

    @Test
    void writesHeaderOnlyWhenNoDataRows() {
        String actual = convert("id,name\n", options(List.of("name")));
        assertEquals("name\n", actual);
    }

    @Test
    void throwsWhenColumnIsMissing() {
        CsvConvertException ex = assertThrows(CsvConvertException.class,
                () -> convert(PEOPLE, options(List.of("missing"))));
        assertEquals("対象カラムが見つかりません: missing（ヘッダ: id, name, age, city）", ex.getMessage());
    }

    @Test
    void convertsFiles(@TempDir Path tempDir) throws IOException {
        Path input = tempDir.resolve("in.csv");
        Path output = tempDir.resolve("out.csv");
        Files.writeString(input, PEOPLE, StandardCharsets.UTF_8);

        try (var in = Files.newInputStream(input); var out = Files.newOutputStream(output)) {
            new CsvConverter().convert(in, out, options(List.of("city")));
        }

        assertEquals("""
                city
                Fukuoka
                Osaka
                Tokyo
                """, Files.readString(output, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    @Test
    void keepsMultipleHeaderRowsAndReversesDataOnly() {
        String input = """
                部門,部門,部門
                id,name,city
                1,Alice,Tokyo
                2,Bob,Osaka
                3,Carol,Fukuoka
                """;
        String actual = convert(input, new ConvertOptions(
                List.of("name", "city"),
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                ',',
                '"',
                2
        ));
        assertEquals("""
                部門,部門
                name,city
                Carol,Fukuoka
                Bob,Osaka
                Alice,Tokyo
                """, actual);
    }

    @Test
    void treatsAllRowsAsHeaderWhenFileIsShorterThanHeaderRows() {
        String actual = convert("title,title\nid,name\n", options(List.of("name"), 3));
        assertEquals("title\nname\n", actual);
    }

    @Test
    void rejectsNegativeHeaderRows() {
        assertThrows(CsvConvertException.class, () -> new ConvertOptions(
                List.of(),
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                ',',
                '"',
                -1
        ));
    }

    private static ConvertOptions options(List<String> columns) {
        return options(columns, 1);
    }

    private static ConvertOptions options(List<String> columns, int headerRows) {
        return new ConvertOptions(columns, StandardCharsets.UTF_8, StandardCharsets.UTF_8, ',', '"', headerRows);
    }

    private static String convert(String csv, ConvertOptions options) {
        return new String(convertBytes(csv.getBytes(options.inputEncoding()), options), options.outputEncoding())
                .replace("\r\n", "\n");
    }

    private static byte[] convertBytes(byte[] input, ConvertOptions options) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            new CsvConverter().convert(new ByteArrayInputStream(input), output, options);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
