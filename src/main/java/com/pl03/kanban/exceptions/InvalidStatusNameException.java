package com.pl03.kanban.exceptions;

public class InvalidStatusNameException extends RuntimeException {
    public InvalidStatusNameException(String message) {
        super(message);
    }
}
