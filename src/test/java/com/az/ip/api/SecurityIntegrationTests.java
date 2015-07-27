package com.az.ip.api;

import com.az.ip.api.model.Error;
import com.az.ip.api.model.Patient;
import com.az.ip.api.persistence.jpa.PatientRepository;
import org.apache.http.NoHttpResponseException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class SecurityIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityIntegrationTests.class);
    private static final String BASE_URI = "/api";
    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

	private RestTemplate restTemplate = null;
    private String baseUrl = null;

    @BeforeClass
    public static void setupSSL() {
        SSLUtil.registerKeyStore("server.jks");
    }

    @Before
    public void setupBaseUrlAndRestTemplate() {
        baseUrl = PROTOCOL + "://localhost:" + port + BASE_URI;
        restTemplate = new TestRestTemplate(user, pwd);
    }

    @Ignore
    @Test
    public void testLoginErrorNoHttps() {
        try {
            // Make a request using http instead of https, expect an error
            ResponseEntity<Patient[]> entity = new TestRestTemplate().getForEntity("http://localhost:" + port + BASE_URI, Patient[].class);
            fail("Expected an error due to http access to a https protected resource");

        } catch (ResourceAccessException ex) {
            // OK, we got en exception when trying to access a https protected resource using plain http
            Throwable cause = ex.getCause();
            assertNotNull(cause);
            assertTrue("Unexpected cause of the exception: " + cause, cause instanceof NoHttpResponseException);
            assertEquals("localhost:" + port + " failed to respond", cause.getMessage());

        } catch (Throwable t) {
            fail("Unexpected exception throwed: " + t);
        }
    }

    @Test
    public void testLoginErrorNoCredentials() {

        // Make a request without credentials, expect 404 (NOT_FOUND) as http response code
        ResponseEntity<String> entity = new TestRestTemplate().getForEntity(baseUrl, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        assertEquals("Basic realm=\"Spring\"", entity.getHeaders().get("WWW-Authenticate").get(0));

        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entity.getBody(), entity.getBody().contains("\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\",\"path\":\"" + BASE_URI + "\""));
    }

    @Test
    public void testLoginErrorInvalidCredentials() {

        // Make a request with invalid credentials, expect 404 (NOT_FOUND) as http response code
        ResponseEntity<String> entity = new TestRestTemplate("non-existing-user", "invalid-password").getForEntity(baseUrl, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        assertEquals("Basic realm=\"Spring\"", entity.getHeaders().get("WWW-Authenticate").get(0));

        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entity.getBody(), entity.getBody().contains("\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Bad credentials\",\"path\":\"" + BASE_URI + "\""));
    }
}