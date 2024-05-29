package com.pl03.kanban.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;


@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleItemNotFoundException(
            ItemNotFoundException ex, WebRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidTaskFieldException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTaskFieldException(InvalidTaskFieldException ex, WebRequest request) {
        return getResponseForFieldsValidation(request, ex.getMessage(), ex.getErrors(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidStatusFieldException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusFieldException(InvalidStatusFieldException ex, WebRequest request) {
        return getResponseForFieldsValidation(request, ex.getMessage(), ex.getErrors(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> getResponseForFieldsValidation(WebRequest request, String message, List<ErrorResponse.ValidationError> validationErrors, HttpStatus status) {
        ErrorResponse errorResponse = new ErrorResponse(status.value(), message, request.getDescription(false));
        errorResponse.setErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, status);
    }
}
