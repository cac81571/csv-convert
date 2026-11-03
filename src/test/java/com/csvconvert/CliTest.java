package com.csvconvert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliTest {
    @Test
    void extractsAndReversesViaCli(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("people.csv");
        Path output = tempDir.resolve("out.csv");
        Files.writeString(input, """
                id,name,age,city
                1,Alice,30,Tokyo
                2,Bob,25,Osaka
                3,Carol,40,Fukuoka
                """, StandardCharsets.UTF_8);

        int code = Cli.commandLine().execute(
                "-c", "name,city",
                "--reverse",
                input.toString(),
                output.toString()
        );

        assertEquals(0, code);
        assertEquals("""
                name,city
                Carol,Fukuoka
                Bob,Osaka
                Alice,Tokyo
                """, Files.readString(output, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    @Test
    void supportsCustomDelimiterQuoteAndEncoding(@TempDir Path tempDir) throws Exception {
        Charset shiftJis = Charset.forName("MS932");
        Path input = tempDir.resolve("people.tsv");
        Path output = tempDir.resolve("out.tsv");
        Files.writeString(input, "名前\tメモ\n佐藤\t'a;b'\n鈴木\t'x,y'\n", shiftJis);

        int code = Cli.commandLine().execute(
                "-c", "メモ,名前",
                "-e", "MS932",
                "-d", "TAB",
                "-q", "SINGLE",
                "--reverse",
                "-o", output.toString(),
                input.toString()
        );

        assertEquals(0, code);
        assertEquals("メモ\t名前\nx,y\t鈴木\na;b\t佐藤\n",
                Files.readString(output, shiftJis).replace("\r\n", "\n"));
    }

    @Test
    void returnsErrorForUnknownColumn(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("in.csv");
        Files.writeString(input, "id,name\n1,Alice\n", StandardCharsets.UTF_8);

        int code = Cli.commandLine().execute("-c", "missing", input.toString());
        assertEquals(1, code);
    }

    @Test
    void keepsSpecifiedHeaderRows(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("multi.csv");
        Path output = tempDir.resolve("out.csv");
        Files.writeString(input, """
                部門,部門,部門
                id,name,city
                1,Alice,Tokyo
                2,Bob,Osaka
                """, StandardCharsets.UTF_8);

        int code = Cli.commandLine().execute(
                "-c", "name,city",
                "-H", "2",
                "--reverse",
                input.toString(),
                output.toString()
        );

        assertEquals(0, code);
        assertEquals("""
                部門,部門
                name,city
                Bob,Osaka
                Alice,Tokyo
                """, Files.readString(output, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    @Test
    void treatsZeroHeaderRowsAsDataOnly(@TempDir Path tempDir) throws Exception {
        Path input = tempDir.resolve("in.csv");
        Path output = tempDir.resolve("out.csv");
        Files.writeString(input, """
                1,Alice
                2,Bob
                """, StandardCharsets.UTF_8);

        int code = Cli.commandLine().execute(
                "-c", "2",
                "--header-rows", "0",
                "--reverse",
                input.toString(),
                output.toString()
        );

        assertEquals(0, code);
        assertEquals("""
                Bob
                Alice
                """, Files.readString(output, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }
}
