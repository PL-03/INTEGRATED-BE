package com.pl03.kanban.exceptions;

public class InvalidTaskTitleException extends RuntimeException {
    public InvalidTaskTitleException(String message) {
        super(message);
    }
}