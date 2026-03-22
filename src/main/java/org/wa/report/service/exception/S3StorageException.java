package org.wa.report.service.exception;

public class S3StorageException extends RuntimeException {

    public S3StorageException(String message, Throwable cause) {
        super(message);
    }
}
