package com.pl03.kanban.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldNameConstants
public class ErrorResponse {
    private final int status;
    private final String message;
    private final String instance;
    private List<ValidationError> errors = new ArrayList<>(); // Initialize with an empty list. in case no errors

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class ValidationError {
        private final String field;
        private final String message;
    }

    public void addValidationError(String field, String message) {
        errors.add(new ValidationError(field, message));
    }
}