package com.pl03.kanban.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<Object> handleItemNotFoundException(
            ItemNotFoundException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).substring(4)); // delete 'uri=' just for the requirement

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidTaskFieldException.class)
    public ResponseEntity<Object> handleInvalidTaskFieldException(InvalidTaskFieldException ex, WebRequest request) {
        return getResponseForFieldsValidation(request, ex.getMessage(), ex.getErrors(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidStatusFieldException.class)
    public ResponseEntity<Object> handleInvalidStatusFieldException(InvalidStatusFieldException ex, WebRequest request) {
        return getResponseForFieldsValidation(request, ex.getMessage(), ex.getErrors(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> getResponseForFieldsValidation(WebRequest request, String message, List<Map<String, String>> errors, HttpStatus status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("instance", request.getDescription(false));
        body.put("error", status.getReasonPhrase());
        body.put("errors", errors);

        return new ResponseEntity<>(body, status);
    }
}
