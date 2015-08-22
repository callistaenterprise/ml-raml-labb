package com.az.ip.api.services.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by magnus on 21/08/15.
 *
 * Exception wrapper for 422, UNPROCESSABLE_ENTITY
 */
@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class HttpUnprocessableEntityException extends RuntimeException {
    public HttpUnprocessableEntityException(String message) {
        super(message);
    }
}
