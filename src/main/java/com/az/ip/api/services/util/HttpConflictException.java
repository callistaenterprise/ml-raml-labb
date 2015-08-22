package com.az.ip.api.services.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by magnus on 21/08/15.
 *
 * Exception wrapper for 409, CONFLICT
 */
@ResponseStatus(value = HttpStatus.CONFLICT)
public class HttpConflictException extends RuntimeException {
    public HttpConflictException(String message) {
        super(message);
    }
}
