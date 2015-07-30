package com.az.ip.api;

import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Patient;
import com.az.ip.api.persistence.jpa.PatientRepository;
import org.junit.Before;
import org.junit.BeforeClass;
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
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class PatientIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(PatientIntegrationTests.class);
    private static final String BASE_URI = "/api/patients";
    private static final String PROTOCOL = "http";

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

    private final static int MIN_PATIENT_NO = 11;
    private final static int MAX_PATIENT_NO = 40;
    private final static int NO_OF_PATIENTS = MAX_PATIENT_NO - MIN_PATIENT_NO + 1;

    @Before
    public void setupDb() {
        repository.deleteAll();
        for (int i = MIN_PATIENT_NO; i <= MAX_PATIENT_NO; i++) {
            repository.save(createTestDbPatient(getUsername(i)));
        }
        LOG.info("Created {} test patients", repository.count());
    }

    @BeforeClass
    public static void setupSSL() {
        SSLUtil.registerKeyStore("server.jks");
    }

    @Before
    public void setupBaseUrlAndRestTemplate() {
        baseUrl = PROTOCOL + "://localhost:" + port + BASE_URI;
        restTemplate = new TestRestTemplate(user, pwd);
    }

    @Test
    public void testPostPatient() {

        Patient newPatient = createTestApiPatient(getUsername((MAX_PATIENT_NO + 1)));
        ResponseEntity entity = restTemplate.postForEntity(baseUrl, newPatient, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNull(entity.getBody());

        // Verify state in db
        assertEquals(NO_OF_PATIENTS + 1, repository.count());
    }

    @Test
    public void testPostPatientDuplicateError() {

        Patient newPatient = createTestApiPatient(getUsername(MIN_PATIENT_NO));
        ResponseEntity<Error> entity = restTemplate.postForEntity(baseUrl, newPatient, Error.class);

        // Verify Rest response
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertNotNull(entity.getBody());
        // TODO: Add verification of the fields content in the Error-object

        // Verify state in db
        assertEquals(NO_OF_PATIENTS, repository.count());
    }

    @Test
    public void testGetPatients() {

        // Ask for all patients, e.g. set size to -1
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1", Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_PATIENTS, body.length);

        // Verify that we got patients with the expected username's, i.e. starting with MIN_PATIENT_NO and in ascending order
        final AtomicInteger userId = new AtomicInteger(MIN_PATIENT_NO);
        Arrays.stream(body).forEach(p -> assertEquals(getUsername(userId.getAndIncrement()), p.getUsername()));

    }

    @Test
    public void testGetPatientsDescending() {

        // Ask for all patients, e.g. set size to -1
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1&orderBy=username&order=desc", Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_PATIENTS, body.length);

        // Verify that we got patients with the expected username's, i.e. starting with MAX_PATIENT_NO and in descending order
        final AtomicInteger userId = new AtomicInteger(MAX_PATIENT_NO);
        Arrays.stream(body).forEach(p -> assertEquals(getUsername(userId.getAndDecrement()), p.getUsername()));

    }

    @Test
    public void testGetPatientsFirstPage() {

        final int SIZE = 3;

        // TODO: Add sort order on ascending username
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?size=" + SIZE, Patient[].class);
        Patient[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first patients as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got patients with the expected username's, i.e. starting with MIN_PATIENT_NO and in ascending order
        final AtomicInteger userId = new AtomicInteger(MIN_PATIENT_NO);
        Arrays.stream(body).forEach(p -> assertEquals(getUsername(userId.getAndIncrement()), p.getUsername()));
    }

    @Test
    public void testGetPatientsLastPage() {

        final int SIZE = 3;

        // TODO: Add sort order on descending username
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?size=" + SIZE + "&orderBy=username&order=desc", Patient[].class);
        Patient[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first patients as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got patients with the expected username's, i.e. starting with MIN_PATIENT_NO and in ascending order
        final AtomicInteger userId = new AtomicInteger(MAX_PATIENT_NO);
        Arrays.stream(body).forEach(p -> assertEquals(getUsername(userId.getAndDecrement()), p.getUsername()));
    }

    @Test
    public void testGetPatientsWithPaging() {

        final int PAGE = 2;
        final int SIZE = 5;

        // TODO: Add sort order on ascending username
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?page=" + PAGE + "&size=" + SIZE, Patient[].class);
        Patient[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first patients as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got patients with the expected username's, i.e. starting with MIN_PATIENT_NO plus the offset given by PAGE and SIZE (skipping PAGE*SIZE patients)
        final AtomicInteger userId = new AtomicInteger(MIN_PATIENT_NO + PAGE*SIZE);
        Arrays.stream(body).forEach(p -> assertEquals(getUsername(userId.getAndIncrement()), p.getUsername()));
    }

    @Test
    public void testGetPatientsWithPagingDescending() {

        final int PAGE = 2;
        final int SIZE = 5;

        // TODO: Add sort order on ascending username
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?page=" + PAGE + "&size=" + SIZE + "&orderBy=username&order=desc", Patient[].class);
        Patient[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first patients as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got patients with the expected username's, i.e. starting with MAX_PATIENT_NO minus the offset given by PAGE and SIZE (skipping PAGE*SIZE patients)
        final AtomicInteger userId = new AtomicInteger(MAX_PATIENT_NO - PAGE*SIZE);
        Arrays.stream(body).forEach(p -> assertEquals(getUsername(userId.getAndDecrement()), p.getUsername()));
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

    @Test
    public void testGetOnePatient() {

        String username = getUsername(MIN_PATIENT_NO);

        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(username, entity.getBody().getUsername());
    }

    @Test
    public void testGetOnePatientNotFoundError() {

        String usernameNotExisting = getUsername(MAX_PATIENT_NO + 1);

        ResponseEntity<String> entity = restTemplate.getForEntity(baseUrl + "/" + usernameNotExisting, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entity.getBody(), entity.getBody().contains("\"status\":404,\"error\":\"Not Found\",\"message\":\"Not Found\",\"path\":\"" + BASE_URI + "/" + usernameNotExisting + "\""));
    }

    @Test
    public void testUpdateOnePatient() {

        String username = getUsername(MIN_PATIENT_NO);

        // Get the patient
        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        Patient p = entity.getBody();
        assertEquals(username, p.getUsername());
        assertEquals(getExpectedFirstname(username), p.getFirstname());
        assertEquals(0, (int)p.getVersion());

        // Update the first name
        p.setFirstname("new-" + p.getFirstname());
        restTemplate.put(baseUrl + "/" + username, p, Patient.class);

        // Get the patient again
        ResponseEntity<Patient> entityUpdated = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response of the updated patient
        assertEquals(HttpStatus.OK, entityUpdated.getStatusCode());
        Patient pUpdated = entityUpdated.getBody();
        assertEquals(username, pUpdated.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), pUpdated.getFirstname());
        assertEquals(1, (int) pUpdated.getVersion());
    }

    @Test
    public void testUpdateOnePatientPessimisticLockError() {

        String username = getUsername(MIN_PATIENT_NO);

        // Get the patient
        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        Patient p = entity.getBody();
        assertEquals(username, p.getUsername());
        assertEquals(getExpectedFirstname(username), p.getFirstname());
        assertEquals(0, (int)p.getVersion());

        // Update the first name
        p.setFirstname("new-" + p.getFirstname());
        restTemplate.put(baseUrl + "/" + username, p, Patient.class);

        // Get the patient again
        ResponseEntity<Patient> entityUpdated = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response of the updated patient
        assertEquals(HttpStatus.OK, entityUpdated.getStatusCode());
        Patient pUpdated = entityUpdated.getBody();
        assertEquals(username, pUpdated.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), pUpdated.getFirstname());
        assertEquals(1, (int) pUpdated.getVersion());




        // Update the patient again using the now outdated entity
        p.setFirstname("2-" + p.getFirstname());

        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + username, p, Patient.class);

        // Get the patient again
        ResponseEntity<Patient> entityUpdatedAgain = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response of the updated patient, i.e. verify that the second stale update did not succeed
        assertEquals(HttpStatus.OK, entityUpdatedAgain.getStatusCode());
        Patient pUpdatedagain = entityUpdatedAgain.getBody();
        assertEquals(username, pUpdatedagain.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), pUpdatedagain.getFirstname());
        assertEquals(1, (int) pUpdatedagain.getVersion());
    }

    @Test
    public void testDeletePatient() {
        String username = getUsername(MIN_PATIENT_NO);

        // Get the patient
        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        Patient p = entity.getBody();
        assertEquals(username, p.getUsername());

        // Delete the patient
        restTemplate.delete(baseUrl + "/" + username);

        // Verify state in db
        assertEquals(NO_OF_PATIENTS - 1, repository.count());

        // Get the patient again, should return a 404, NOT_FOUND
        ResponseEntity<String> entityRemoved = restTemplate.getForEntity(baseUrl + "/" + username, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entityRemoved.getStatusCode());
        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entityRemoved.getBody(), entityRemoved.getBody().contains("\"status\":404,\"error\":\"Not Found\",\"message\":\"Not Found\",\"path\":\"" + BASE_URI + "/" + username + "\""));
    }

    @Test
    public void testDeleteNotExistingPatient() {

        String usernameNotExisting = getUsername(MAX_PATIENT_NO + 1);

        // Verify state in db
        assertEquals(NO_OF_PATIENTS, repository.count());

        // Try to delete the non-existing patient
        restTemplate.delete(baseUrl + "/" + usernameNotExisting);

        // Verify state in db, i.e. no change
        assertEquals(NO_OF_PATIENTS, repository.count());
    }

    private String getExpectedFirstname(String username) {
        return "F1";
    }

    private String getUsername(int i) {
        return "U" + i;
    }

    private com.az.ip.api.persistence.jpa.Patient createTestDbPatient(String username) {
        return new com.az.ip.api.persistence.jpa.Patient(username, "1234", "F1", "L1", 100, 200);
    }

    private Patient createTestApiPatient(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }

}