package com.cloudidentity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class AWSIntegrationException extends RuntimeException {
    public AWSIntegrationException(String message) {
        super(message);
    }
    public AWSIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
