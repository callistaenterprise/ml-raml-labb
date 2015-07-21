package com.az.ip.api;

import com.az.ip.api.model.*;
import com.az.ip.api.model.Error;
import com.az.ip.api.persistence.jpa.PatientRepository;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class PatientIntegrationTests {

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

    @Inject
    PatientRepository repository;

	private RestTemplate restTemplate = null;
    private String baseUrl = null;

    @Before
    public void setupDb() {
        repository.deleteAll();
        repository.save(createDbPatient("U11"));
        repository.save(createDbPatient("U21"));
        repository.save(createDbPatient("U31"));
    }

    @BeforeClass
    public static void setupSSL() {
        registerKeyStore("server.jks");
    }

    @Before
    public void setupBaseUrlAndRestTemplate() {
        baseUrl = "https://localhost:" + port + "/patients";
        restTemplate = new TestRestTemplate(user, pwd);
    }

    @Test
    public void testLoginErrorNoHttps() {
        try {
            String baseUrlNoHttps = "http://localhost:" + port + "/patients";
            RestTemplate invalidRestTemplate = new TestRestTemplate();
            ResponseEntity<Patient[]> entity = invalidRestTemplate.getForEntity(baseUrlNoHttps, Patient[].class);
            fail("Expected an error due to http access to a https protected resource");
        } catch (ResourceAccessException ex) {
            // OK, we got en exception when trying to access a https protected resource using plain http
        }
    }

    @Test
    public void testLoginErrorNoCredentials() {
        RestTemplate invalidRestTemplate = new TestRestTemplate();
        ResponseEntity<Patient[]> entity = invalidRestTemplate.getForEntity(baseUrl, Patient[].class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        assertNull(entity.getBody());
        assertEquals("Basic realm=\"Spring\"", entity.getHeaders().get("WWW-Authenticate").get(0));
    }

    @Test
    public void testLoginErrorInvalidCredentials() {
        RestTemplate invalidRestTemplate = new TestRestTemplate("non-exisintg-user", "invalid-password");
        ResponseEntity<Patient[]> entity = invalidRestTemplate.getForEntity(baseUrl, Patient[].class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        assertNull(entity.getBody());
        assertEquals("Basic realm=\"Spring\"", entity.getHeaders().get("WWW-Authenticate").get(0));
    }

    @Test
    public void testPostPatientOk() {

        String username = "U41";

        Patient newPatient = createRestPatient(username);
        ResponseEntity entity = restTemplate.postForEntity(baseUrl, newPatient, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNull(entity.getBody());

        // Verify state in db
        assertEquals(4, getDbCnt());
    }

    @Test
    public void testPostPatientDuplicateError() {

        String username = "U11";

        Patient newPatient = createRestPatient(username);
        ResponseEntity<Error> entity = restTemplate.postForEntity(baseUrl, newPatient, Error.class);

        // Verify Rest response
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertNotNull(entity.getBody());
        // TODO: Add verification of the fields content in the Error-object

        // Verify state in db
        assertEquals(3, getDbCnt());
    }

    @Test
    public void testGetPatientOk() {

        String username = "U21";

        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(username, entity.getBody().getUsername());
    }

    @Test
    public void testGetPatientNotFoundError() {

        String username = "U99";

        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        assertNull(entity.getBody());
    }

    @Test
    public void testGetPatientsOk() {

        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl, Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(3, body.length);
    }

    @Test
    public void testGetPatientsNoFound() {

        repository.deleteAll();

        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl, Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(0, body.length);
    }

    private int getDbCnt() {
        int cnt = 0;
        for (Object p: repository.findAll()) cnt++;
        return cnt;
    }

    private com.az.ip.api.persistence.jpa.Patient createDbPatient(String username) {
        return new com.az.ip.api.persistence.jpa.Patient(username, "1234", "F1", "L1", 100, 200);
    }

    private Patient createRestPatient(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }

    private static void registerKeyStore(String keyStoreName) {
        try {
            System.err.println("### Load certs from classpath: '" + keyStoreName + "'");

            ClassLoader classLoader = PatientIntegrationTests.class.getClassLoader();
            InputStream keyStoreInputStream = classLoader.getResourceAsStream(keyStoreName);
            if (keyStoreInputStream == null) {
                throw new FileNotFoundException("Could not find file named '" + keyStoreName + "' in the CLASSPATH");
            }

            //load the keystore
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(keyStoreInputStream, null);

            //add to known keystore
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            //default SSL connections are initialized with the keystore above
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagers, null);
            SSLContext.setDefault(sc);
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("### ERR: " + e);
            throw new RuntimeException(e);
        }
    }
}