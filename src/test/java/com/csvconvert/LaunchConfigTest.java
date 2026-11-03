package com.csvconvert;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LaunchConfigTest {
    @Test
    void buildsMinimalArgsWithDefaultsOmitted() {
        LaunchConfig config = new LaunchConfig(
                "app.jar",
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
        assertEquals(List.of("-c", "name,city"), config.toCliArgs());
    }

    @Test
    void includesNonDefaultOptionsUsingAliases() {
        LaunchConfig config = new LaunchConfig(
                "app.jar",
                "2,4",
                2,
                "UTF-8",
                "MS932",
                "UTF-8",
                "\t",
                "'",
                true,
                false
        );
        assertEquals(
                List.of(
                        "-c", "2,4",
                        "-H", "2",
                        "--input-encoding", "MS932",
                        "--output-encoding", "UTF-8",
                        "-d", "TAB",
                        "-q", "SINGLE",
                        "--reverse"
                ),
                config.toCliArgs()
        );
    }

    @Test
    void omitsColumnsWhenBlank() {
        LaunchConfig config = LaunchConfig.defaults();
        assertFalse(config.toCliArgs().contains("-c"));
        assertFalse(config.toCliArgs().contains("--reverse"));
    }
}
