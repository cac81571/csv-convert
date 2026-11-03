package com.csvconvert.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatchBuilderFrameTest {
    @Test
    void normalizesBatFileName() {
        assertEquals("run-csv-convert.bat", BatchBuilderFrame.normalizeBatFileName("run-csv-convert.bat"));
        assertEquals("run.bat", BatchBuilderFrame.normalizeBatFileName("run"));
        assertEquals("job.bat", BatchBuilderFrame.normalizeBatFileName("C:\\tmp\\job.bat"));
        assertEquals("job.bat", BatchBuilderFrame.normalizeBatFileName("folder/job"));
    }

    @Test
    void rejectsInvalidBatFileName() {
        assertThrows(IllegalArgumentException.class, () -> BatchBuilderFrame.normalizeBatFileName(""));
        assertThrows(IllegalArgumentException.class, () -> BatchBuilderFrame.normalizeBatFileName("a:b.bat"));
        assertThrows(IllegalArgumentException.class, () -> BatchBuilderFrame.normalizeBatFileName(".."));
    }
}
