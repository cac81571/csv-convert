package com.csvconvert;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Builds a Windows {@code .bat} launcher from {@link LaunchConfig}.
 * <ul>
 *   <li>JAR path is relative to the bat file ({@code cd /d "%~dp0"}).</li>
 *   <li>Input is the first argument ({@code %1}), for drag-and-drop.</li>
 *   <li>Output is {@code <inputName>_conv.<ext>} beside the input.</li>
 * </ul>
 */
public final class BatchScriptBuilder {
    public static final String INPUT_VAR = "INPUT";
    public static final String OUTPUT_VAR = "OUTPUT";

    private static final Pattern WINDOWS_ABS = Pattern.compile("^[A-Za-z]:[\\\\/].*");
    private static final Pattern UNC_ABS = Pattern.compile("^\\\\\\\\.*");

    /** JVM flags so Japanese CLI messages match UTF-8 console (chcp 65001). */
    static final String JAVA_CONSOLE_OPTS = "-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8";

    public String build(LaunchConfig config) {
        // Comments may include Japanese; write the bat as MS932 from the GUI.
        // Keep runtime echo/java lines ASCII-safe. Put rem settings before chcp 65001.
        StringBuilder bat = new StringBuilder();
        bat.append("@echo off\r\n");
        bat.append("setlocal\r\n");
        bat.append(settingsComments(config));
        bat.append("cd /d \"%~dp0\"\r\n");
        bat.append("chcp 65001 >nul\r\n");
        bat.append("\r\n");
        bat.append("if \"%~1\"==\"\" (\r\n");
        bat.append("  echo Usage: drag and drop a CSV file onto this bat.\r\n");
        bat.append("  echo Or: %~nx0 \"input.csv\"\r\n");
        bat.append("  pause\r\n");
        bat.append("  exit /b 1\r\n");
        bat.append(")\r\n");
        bat.append("\r\n");
        bat.append("set \"").append(INPUT_VAR).append("=%~1\"\r\n");
        bat.append("set \"").append(OUTPUT_VAR).append("=%~dpn1_conv%~x1\"\r\n");
        bat.append("\r\n");
        bat.append("echo Input : %").append(INPUT_VAR).append("%\r\n");
        bat.append("echo Output: %").append(OUTPUT_VAR).append("%\r\n");
        bat.append(javaCommand(config, "\"%" + INPUT_VAR + "%\"", "\"%" + OUTPUT_VAR + "%\"")).append("\r\n");
        bat.append("set EXITCODE=%ERRORLEVEL%\r\n");
        if (config.pauseAtEnd()) {
            bat.append("echo.\r\n");
            bat.append("echo Exit code: %EXITCODE%\r\n");
            bat.append("pause\r\n");
        }
        bat.append("exit /b %EXITCODE%\r\n");
        return bat.toString();
    }

    static String settingsComments(LaunchConfig config) {
        String columns = config.columns().isBlank() ? "(all columns)" : config.columns().strip();
        String encoding;
        if (!config.inputEncoding().isBlank() || !config.outputEncoding().isBlank()) {
            String inEnc = config.inputEncoding().isBlank() ? config.encoding() : config.inputEncoding().strip();
            String outEnc = config.outputEncoding().isBlank() ? config.encoding() : config.outputEncoding().strip();
            encoding = "input=" + inEnc + ", output=" + outEnc;
        } else {
            encoding = config.encoding();
        }

        StringBuilder rem = new StringBuilder();
        rem.append(remLine("=== csv-convert settings ==="));
        rem.append(remLine("jar=" + toBatRelativeJar(config.jarPath())));
        rem.append(remLine("columns=" + columns));
        rem.append(remLine("header-rows=" + config.headerRows()));
        rem.append(remLine("encoding=" + encoding));
        rem.append(remLine("delimiter=" + LaunchConfig.normalizeDelimiter(config.delimiter())));
        rem.append(remLine("quote=" + LaunchConfig.normalizeQuote(config.quote())));
        rem.append(remLine("reverse=" + config.reverseRows()));
        rem.append(remLine("pause=" + config.pauseAtEnd()));
        rem.append(remLine("input=%1 (drag and drop)"));
        rem.append(remLine("output=%~dpn1_conv%~x1"));
        rem.append(remLine("cli-args=" + String.join(" ", config.toCliArgs())));
        rem.append("\r\n");
        return rem.toString();
    }

    private static String remLine(String text) {
        // Escape % so cmd does not expand variables inside comments.
        String safe = text
                .replace("\r", " ")
                .replace("\n", " ")
                .replace("%", "%%");
        return "rem " + safe + "\r\n";
    }

    public String previewCommandLine(LaunchConfig config) {
        return javaCommand(config, "\"%INPUT%\"", "\"%OUTPUT%\"");
    }

    private static String javaCommand(LaunchConfig config, String inputArg, String outputArg) {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add(JAVA_CONSOLE_OPTS);
        command.add("-jar");
        command.add(quoteForCmd(toBatRelativeJar(config.jarPath())));
        for (String arg : config.toCliArgs()) {
            command.add(quoteForCmd(arg));
        }
        command.add(inputArg);
        command.add(outputArg);
        return String.join(" ", command);
    }

    /**
     * Makes a jar path relative to {@code batFile}'s directory.
     * Falls back to the jar file name when a relative path cannot be computed.
     */
    public static String jarPathRelativeToBat(Path jarFile, Path batFile) {
        Path jar = jarFile.toAbsolutePath().normalize();
        Path batDir = batFile.toAbsolutePath().normalize().getParent();
        if (batDir == null) {
            return jar.getFileName().toString();
        }
        try {
            return toWindowsPath(batDir.relativize(jar));
        } catch (IllegalArgumentException ex) {
            return jar.getFileName().toString();
        }
    }

    /**
     * Ensures the jar path written into a bat is never absolute.
     * Absolute inputs are reduced to the file name (assumes jar sits next to the bat).
     */
    public static String toBatRelativeJar(String jarPath) {
        String path = sanitizePathText(jarPath);
        if (path.isEmpty()) {
            return "csv-convert-1.0.0.jar";
        }
        Path asPath = Path.of(path);
        if (isAbsolutePath(path, asPath)) {
            Path name = asPath.getFileName();
            return name == null ? "csv-convert-1.0.0.jar" : name.toString();
        }
        return toWindowsPath(asPath.normalize());
    }

    /** @deprecated use {@link #toBatRelativeJar(String)} */
    static String relativeJar(String jarPath) {
        return toBatRelativeJar(jarPath);
    }

    public static boolean isAbsolutePath(String path, Path asPath) {
        if (asPath.isAbsolute()) {
            return true;
        }
        String normalized = path.replace('/', '\\');
        return WINDOWS_ABS.matcher(normalized).matches() || UNC_ABS.matcher(normalized).matches();
    }

    private static String sanitizePathText(String jarPath) {
        if (jarPath == null) {
            return "";
        }
        String path = jarPath.strip();
        if (path.length() >= 2 && path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1).strip();
        }
        if (path.toLowerCase(Locale.ROOT).startsWith("file:")) {
            try {
                path = Path.of(java.net.URI.create(path)).toString();
            } catch (RuntimeException ignored) {
                // keep original
            }
        }
        return path;
    }

    private static String toWindowsPath(Path path) {
        return path.toString().replace('/', '\\');
    }

    /**
     * Quotes a single cmd.exe argument. Internal double quotes become {@code ""}.
     */
    static String quoteForCmd(String value) {
        if (value == null) {
            return "\"\"";
        }
        boolean needsQuotes = value.isEmpty()
                || value.indexOf(' ') >= 0
                || value.indexOf('\t') >= 0
                || value.indexOf('&') >= 0
                || value.indexOf('^') >= 0
                || value.indexOf('<') >= 0
                || value.indexOf('>') >= 0
                || value.indexOf('|') >= 0
                || value.indexOf('%') >= 0
                || value.indexOf('!') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf(',') >= 0
                || value.indexOf(';') >= 0
                || value.indexOf('=') >= 0;

        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
