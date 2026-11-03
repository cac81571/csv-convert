package com.csvconvert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchScriptBuilderTest {
    @Test
    void buildsBatchForDragAndDropWithConvOutput() {
        LaunchConfig config = new LaunchConfig(
                "csv-convert-1.0.0.jar",
                "name,city",
                1,
                "UTF-8",
                "",
                "",
                ",",
                "\"",
                false,
                true
        );

        String bat = new BatchScriptBuilder().build(config);
        assertTrue(bat.startsWith("@echo off\r\n"));
        assertTrue(bat.contains("rem === csv-convert settings ===\r\n"));
        assertTrue(bat.contains("rem jar=csv-convert-1.0.0.jar\r\n"));
        assertTrue(bat.contains("rem columns=name,city\r\n"));
        assertTrue(bat.contains("rem header-rows=1\r\n"));
        assertTrue(bat.contains("rem encoding=UTF-8\r\n"));
        assertTrue(bat.contains("rem delimiter=COMMA\r\n"));
        assertTrue(bat.contains("rem quote=DOUBLE\r\n"));
        assertTrue(bat.contains("rem reverse=false\r\n"));
        assertTrue(bat.contains("rem input=%%1 (drag and drop)\r\n"));
        assertTrue(bat.contains("rem output=%%~dpn1_conv%%~x1\r\n"));
        assertTrue(bat.contains("rem cli-args=-c name,city\r\n"));
        assertTrue(bat.contains("cd /d \"%~dp0\"\r\n"));
        assertTrue(bat.contains("if \"%~1\"==\"\" (\r\n"));
        assertTrue(bat.contains("set \"INPUT=%~1\"\r\n"));
        assertTrue(bat.contains("set \"OUTPUT=%~dpn1_conv%~x1\"\r\n"));
        assertTrue(bat.contains("chcp 65001 >nul\r\n"));
        assertTrue(bat.contains(
                "java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar csv-convert-1.0.0.jar -c \"name,city\" \"%INPUT%\" \"%OUTPUT%\"\r\n"));
        assertTrue(bat.contains("pause\r\n"));
    }

    @Test
    void forcesAbsoluteJarToFileNameRelativeToBat() {
        assertEquals("app.jar", BatchScriptBuilder.toBatRelativeJar("C:\\tools\\app.jar"));
        assertEquals("app.jar", BatchScriptBuilder.toBatRelativeJar("\"C:\\tools\\app.jar\""));
        assertEquals("lib\\app.jar", BatchScriptBuilder.toBatRelativeJar("lib/app.jar"));
    }

    @Test
    void neverWritesAbsoluteJarIntoBatch() {
        LaunchConfig config = new LaunchConfig(
                "C:\\Users\\demo\\csv-convert-1.0.0.jar",
                "name",
                1,
                "UTF-8",
                "",
                "",
                ",",
                "\"",
                true,
                false
        );
        String bat = new BatchScriptBuilder().build(config);
        assertTrue(bat.contains("java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar csv-convert-1.0.0.jar -c name"));
        assertTrue(!bat.contains("C:\\Users\\demo"));
        assertEquals("csv-convert-1.0.0.jar", config.jarPath());
    }

    @Test
    void computesJarRelativeToBatDirectory(@TempDir Path tempDir) {
        Path bat = tempDir.resolve("scripts").resolve("run.bat");
        Path jar = tempDir.resolve("dist").resolve("csv-convert-1.0.0.jar");
        String relative = BatchScriptBuilder.jarPathRelativeToBat(jar, bat);
        assertEquals("..\\dist\\csv-convert-1.0.0.jar", relative);
    }

    @Test
    void quotesSpecialCmdCharacters() {
        assertEquals("\"a b\"", BatchScriptBuilder.quoteForCmd("a b"));
        assertEquals("\"a\"\"b\"", BatchScriptBuilder.quoteForCmd("a\"b"));
        assertEquals("-c", BatchScriptBuilder.quoteForCmd("-c"));
        assertEquals("\"name,city\"", BatchScriptBuilder.quoteForCmd("name,city"));
    }

    @Test
    void previewUsesInputOutputPlaceholders() {
        LaunchConfig config = new LaunchConfig(
                "app.jar",
                "",
                0,
                "MS932",
                "",
                "",
                "SEMI",
                "NONE",
                false,
                false
        );
        String preview = new BatchScriptBuilder().previewCommandLine(config);
        assertEquals(
                "java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar app.jar -H 0 -e MS932 -d SEMI -q NONE \"%INPUT%\" \"%OUTPUT%\"",
                preview);
    }
}
