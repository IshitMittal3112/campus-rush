package com.campusrush.core.exceptions;

public class CampusRushException extends Exception {

    public CampusRushException(String message) {
        super(message);
    }

    public CampusRushException(String message, Throwable cause) {
        super(message, cause);
    }
}
