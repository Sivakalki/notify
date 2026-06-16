package com.notify.backend.exception;

public class CohortNotFoundException extends RuntimeException {

    public CohortNotFoundException(String message) {
        super(message);
    }
}
