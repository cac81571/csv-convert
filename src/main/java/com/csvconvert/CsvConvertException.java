package com.csvconvert;

public class CsvConvertException extends RuntimeException {
    public CsvConvertException(String message) {
        super(message);
    }

    public CsvConvertException(String message, Throwable cause) {
        super(message, cause);
    }
}
