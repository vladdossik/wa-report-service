package org.wa.report.service.exception;

import org.springframework.http.HttpStatusCode;

public class StorageServiceException extends RuntimeException {

    public StorageServiceException(String message) {
        super(message);
    }

    public StorageServiceException(String message, Throwable e) {
        super(message);
    }

    public StorageServiceException(String message, HttpStatusCode httpStatusCode) {
        super(message);
    }
}
