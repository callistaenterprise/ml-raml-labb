package com.az.ip.api.services.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Created by magnus on 21/08/15.
 */
@Component
public class SpringMvcUtil {
    public <T> ResponseEntity<T> createOkResponse(T body) {
        return this.createResponse(body, HttpStatus.OK);
    }

    public <T> ResponseEntity<T> createResponse(ResponseEntity<T> result) {
        ResponseEntity response = this.createResponse(result.getBody(), result.getStatusCode());
        return response;
    }

    public <T> ResponseEntity<T> createResponse(T body, HttpStatus httpStatus) {
        return new ResponseEntity(body, httpStatus);
    }
}
