package com.csvconvert;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CsvConverter {
    public void convert(InputStream input, OutputStream output, ConvertOptions options) throws IOException {
        Reader reader = new InputStreamReader(skipUtf8Bom(input, options.inputEncoding()), options.inputEncoding());
        Writer writer = new OutputStreamWriter(output, options.outputEncoding());
        convert(reader, writer, options);
    }

    public void convert(Reader reader, Writer writer, ConvertOptions options) throws IOException {
        try (CSVParser parser = options.csvFormat().parse(uncloseable(reader))) {
            List<CSVRecord> allRows = new ArrayList<>();
            parser.iterator().forEachRemaining(allRows::add);

            int headerCount = Math.min(options.headerRows(), allRows.size());
            List<CSVRecord> headerRows = List.copyOf(allRows.subList(0, headerCount));
            List<CSVRecord> dataRows = new ArrayList<>(allRows.subList(headerCount, allRows.size()));
            ColumnSelection selection = ColumnSelection.resolve(headerRows, dataRows, options);
            if (options.reverseRows()) {
                Collections.reverse(dataRows);
            }

            try (CSVPrinter printer = new CSVPrinter(uncloseable(writer), options.csvFormat())) {
                for (CSVRecord row : headerRows) {
                    printer.printRecord(selection.extract(row));
                }
                for (CSVRecord row : dataRows) {
                    printer.printRecord(selection.extract(row));
                }
            }
        }
        writer.flush();
    }

    static InputStream skipUtf8Bom(InputStream input, Charset charset) throws IOException {
        if (!isUtf8(charset)) {
            return input;
        }
        PushbackInputStream pushback = new PushbackInputStream(input, 3);
        byte[] bom = new byte[3];
        int read = pushback.read(bom);
        if (read == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            return pushback;
        }
        if (read > 0) {
            pushback.unread(bom, 0, read);
        }
        return pushback;
    }

    private static boolean isUtf8(Charset charset) {
        return StandardCharsets.UTF_8.equals(charset) || charset.name().equalsIgnoreCase("UTF8");
    }

    private static Reader uncloseable(Reader reader) {
        return new FilterReader(reader) {
            @Override
            public void close() {
                // Caller owns the reader.
            }
        };
    }

    private static Writer uncloseable(Writer writer) {
        return new FilterWriter(writer) {
            @Override
            public void close() throws IOException {
                flush();
            }
        };
    }
}
