package com.pl03.kanban.exceptions;

import lombok.Getter;

import java.util.List;


@Getter
public class InvalidStatusFieldException extends RuntimeException {
    private List<ErrorResponse.ValidationError> errors;

    public InvalidStatusFieldException(String message, List<ErrorResponse.ValidationError> validationErrors) {
        super(message);
        this.errors = validationErrors;
    }

    public InvalidStatusFieldException(String message) {
        super(message);
    }
}
