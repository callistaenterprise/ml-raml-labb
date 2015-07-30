package com.az.ip.api;

import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * Created by magnus on 30/07/15.
 */
public abstract class AbstractIntegrationTest {

    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    private int port;

    @Value("${mysuer:demo}")
    private String user;

    @Value("${mypwd:omed.1}")
    private String pwd;

    protected RestTemplate restTemplate = null;
    protected String baseUrl = null;

    @BeforeClass
    public static void setupSSL() {
        SSLUtil.registerKeyStore("server.jks");
    }

    protected AbstractIntegrationTest(String baseUri) {
        baseUrl = PROTOCOL + "://localhost:" + port + baseUri;
        restTemplate = new TestRestTemplate(user, pwd);
    }
}
