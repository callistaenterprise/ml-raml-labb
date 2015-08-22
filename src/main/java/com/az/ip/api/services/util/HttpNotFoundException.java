package com.az.ip.api.services.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by magnus on 21/08/15.
 *
 * Exception wrapper for 404, NOT_FOUND
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class HttpNotFoundException extends RuntimeException {
    public HttpNotFoundException(String message) {
        super(message);
    }
}
