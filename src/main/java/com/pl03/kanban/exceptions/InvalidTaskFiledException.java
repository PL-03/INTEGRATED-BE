package com.pl03.kanban.exceptions;

import java.util.List;
import java.util.Map;

public class InvalidTaskFiledException extends RuntimeException {
    private List<Map<String, String>> errors;

    public InvalidTaskFiledException(String message, List<Map<String, String>> errors) {
        super(message);
        this.errors = errors;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }
}