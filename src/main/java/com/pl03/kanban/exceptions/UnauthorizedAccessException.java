package com.pl03.kanban.exceptions;

import lombok.Getter;

import java.util.List;

@Getter
public class UnauthorizedAccessException extends RuntimeException{
    private final List<ErrorResponse.ValidationError> errors;

    public UnauthorizedAccessException(String message, List<ErrorResponse.ValidationError> validationErrors) {
        super(message);
        this.errors = validationErrors;
    }
}
