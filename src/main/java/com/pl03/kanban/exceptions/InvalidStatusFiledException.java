package com.pl03.kanban.exceptions;

public class InvalidStatusFiledException extends RuntimeException {
    public InvalidStatusFiledException(String message) {
        super(message);
    }
}
