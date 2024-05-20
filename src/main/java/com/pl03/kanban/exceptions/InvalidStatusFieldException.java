package com.pl03.kanban.exceptions;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class InvalidStatusFieldException extends RuntimeException {
    private List<Map<String, String>> errors;

    public InvalidStatusFieldException(String message) {
        super(message);
    }

    public InvalidStatusFieldException(String message, List<Map<String, String>> errors) {
        super(message);
        this.errors = errors;
    }

}
