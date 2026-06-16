package com.notify.backend.exception;

public class DlqEventNotFoundException extends RuntimeException {

    public DlqEventNotFoundException(String message) {
        super(message);
    }
}