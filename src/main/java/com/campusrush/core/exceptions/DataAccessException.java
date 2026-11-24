package com.campusrush.core.exceptions;

public class DataAccessException extends CampusRushException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
