package com.pl03.kanban.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
        body.put("path", request.getDescription(false).substring(4)); // ลบ 'uri=' ให้ตรงกับ requirement

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidTaskTitleException.class)
    public ResponseEntity<String> handleInvalidTaskTitleException(InvalidTaskTitleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusNameException.class)
    public ResponseEntity<String> handleInvalidStatusNameException(InvalidStatusNameException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
