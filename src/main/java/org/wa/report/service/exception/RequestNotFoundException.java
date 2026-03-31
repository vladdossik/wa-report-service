package org.wa.report.service.exception;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(String message) {
        super(message);
    }
}
