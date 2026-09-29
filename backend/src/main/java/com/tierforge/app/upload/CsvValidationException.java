package com.tierforge.app.upload;

import java.util.List;

public class CsvValidationException extends RuntimeException {

    private final List<String> errors;

    public CsvValidationException(List<String> errors) {
        super("CSV validation failed: " + String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
