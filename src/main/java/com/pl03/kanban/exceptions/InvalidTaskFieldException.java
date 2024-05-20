package com.pl03.kanban.exceptions;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class InvalidTaskFieldException extends RuntimeException {
    private List<Map<String, String>> errors;

    public InvalidTaskFieldException(String message, List<Map<String, String>> errors) {
        super(message);
        this.errors = errors;
    }

    public InvalidTaskFieldException(String message) {
        super(message);
    }
}