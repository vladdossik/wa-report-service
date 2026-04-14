package org.wa.report.service.exception;

public class ConnectException extends RuntimeException {

    public ConnectException(String message) {
        super(message);
    }

    public ConnectException(String message, Throwable cause) {
        super(message, cause);
    }
}
