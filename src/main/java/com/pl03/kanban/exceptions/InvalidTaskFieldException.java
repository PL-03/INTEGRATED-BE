package com.pl03.kanban.exceptions;

import lombok.Getter;

import java.util.List;

@Getter
public class InvalidTaskFieldException extends RuntimeException {
    private List<ErrorResponse.ValidationError> errors;

    public InvalidTaskFieldException(String message, List<ErrorResponse.ValidationError> validationErrors) {
        super(message);
        this.errors = validationErrors;
    }

    public InvalidTaskFieldException(String message) {
        super(message);
    }
}