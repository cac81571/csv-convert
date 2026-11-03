package com.csvconvert;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Aligns Java stdout/stderr with a UTF-8 Windows console ({@code chcp 65001}).
 * Without this, Japanese messages can garble when {@code stdout.encoding} is MS932
 * but the terminal expects UTF-8.
 */
public final class ConsoleEncoding {
    private ConsoleEncoding() {
    }

    public static void install() {
        Charset charset = resolveConsoleCharset();
        System.setOut(new PrintStream(System.out, true, charset));
        System.setErr(new PrintStream(System.err, true, charset));
    }

    private static Charset resolveConsoleCharset() {
        String stdout = System.getProperty("stdout.encoding");
        if (stdout != null && !stdout.isBlank()) {
            try {
                return Charset.forName(stdout);
            } catch (RuntimeException ignored) {
                // fall through
            }
        }
        return StandardCharsets.UTF_8;
    }
}
