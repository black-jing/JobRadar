package com.jobradar.messaging;

public class RetryableJobAnalysisException extends RuntimeException {
    public RetryableJobAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
