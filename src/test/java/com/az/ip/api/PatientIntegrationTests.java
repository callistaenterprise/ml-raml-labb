package com.az.ip.api;

import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Patient;
import com.az.ip.api.gen.model.Study;
import com.az.ip.api.persistence.jpa.JpaPatient;
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

    private final static int MIN_NO = 11;
    private final static int MAX_NO = 40;
    private final static int NO_OF_ENTITIES = MAX_NO - MIN_NO + 1;

    @Before
    public void setupDb() {
        repository.deleteAll();

        // Insert NO_OF_ENTITIES entities in the database
        for (int i = MIN_NO; i <= MAX_NO; i++) {
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

        Patient newEntity = createTestApiEntity(getUsername((MAX_NO + 1)));
        ResponseEntity entity = restTemplate.postForEntity(baseUrl, newEntity, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNull(entity.getBody());

        // Verify state in db
        assertEquals(NO_OF_ENTITIES + 1, repository.count());
    }

    @Test
    public void testPostPatientDuplicateError() {

        Patient newEntity = createTestApiEntity(getUsername(MIN_NO));
        ResponseEntity<Error> entity = restTemplate.postForEntity(baseUrl, newEntity, Error.class);

        // Verify Rest response
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertNotNull(entity.getBody());
        // TODO: Add verification of the fields content in the Error-object

        // Verify state in db, i.e. no new entity in the database
        assertEquals(NO_OF_ENTITIES, repository.count());
    }

    @Test
    public void testGetPatients() {

        // Ask for all patients, e.g. set size to -1
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1", Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_ENTITIES, body.length);

        // Verify that we got patients with the expected usernames, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MIN_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndIncrement()), e.getUsername()));

    }

    @Test
    public void testGetPatientsDescending() {

        // Ask for all patients in descending order, e.g. set size to -1
        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1&orderBy=username&order=desc", Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_ENTITIES, body.length);

        // Verify that we got patients with the expected usernames, i.e. starting with MAX_NO and in descending order
        final AtomicInteger id = new AtomicInteger(MAX_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndDecrement()), e.getUsername()));

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

        // Verify that we got patients with the expected username's, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MIN_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndIncrement()), e.getUsername()));
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

        // Verify that we got patients with the expected username's, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MAX_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndDecrement()), e.getUsername()));
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

        // Verify that we got patients with the expected username's, i.e. starting with MIN_NO plus the offset given by PAGE and SIZE (skipping PAGE*SIZE patients)
        final AtomicInteger id = new AtomicInteger(MIN_NO + PAGE*SIZE);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndIncrement()), e.getUsername()));
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

        // Verify that we got patients with the expected username's, i.e. starting with MAX_NO minus the offset given by PAGE and SIZE (skipping PAGE*SIZE patients)
        final AtomicInteger id = new AtomicInteger(MAX_NO - PAGE*SIZE);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndDecrement()), e.getUsername()));
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
    public void testGetOnePatientByName() {

        // The helper method implement the whole test already...
        lookupEntityByUsername(getUsername(MIN_NO));
    }

    @Test
    public void testGetOnePatientById() {

        // First get the id of an entity with a known name
        String username = getUsername(MIN_NO);
        String id       = lookupEntityByUsername(username).getId();

        // Now, perform the actual test
        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + id, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(username, entity.getBody().getUsername());
        assertEquals(id, entity.getBody().getId());
    }

    @Test
    public void testGetOnePatientNotFoundError() {

        String idNotExisting = "NON-EXISTING-ID";

        ResponseEntity<String> entity = restTemplate.getForEntity(baseUrl + "/" + idNotExisting, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entity.getBody(), entity.getBody().contains("\"status\":404,\"error\":\"Not Found\",\"message\":\"Not Found\",\"path\":\"" + BASE_URI + "/" + idNotExisting + "\""));
    }

    @Test
    public void testUpdateOnePatient() {

        String username = getUsername(MIN_NO);

        // Get the patient
        Patient entity = lookupEntityByUsername(username);

        // Verify Rest response
        assertEquals(username, entity.getUsername());
        assertEquals(getExpectedFirstname(username), entity.getFirstname());
        assertEquals(0, (int)entity.getVersion());

        // Update the first name
        // TODO how do I check the result of the update???
        entity.setFirstname("new-" + entity.getFirstname());
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Patient.class);

        // Get the patient again
        Patient entityUpdated = lookupEntityByUsername(username);

        // Verify Rest response of the updated patient
        assertEquals(username, entityUpdated.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), entityUpdated.getFirstname());
        assertEquals(1, (int) entityUpdated.getVersion());
    }

    @Test
    public void testUpdateOnePatientPessimisticLockError() {

        String username = getUsername(MIN_NO);

        // Get the patient
        Patient entity = lookupEntityByUsername(username);

        // Verify Rest response
        assertEquals(username, entity.getUsername());
        assertEquals(getExpectedFirstname(username), entity.getFirstname());
        assertEquals(0, (int) entity.getVersion());

        // Update the first name
        entity.setFirstname("new-" + entity.getFirstname());
        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Patient.class);

        // Get the patient again
        Patient entityUpdated = lookupEntityByUsername(username);

        // Verify Rest response of the updated patient
        assertEquals(username, entityUpdated.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), entityUpdated.getFirstname());
        assertEquals(1, (int) entityUpdated.getVersion());



        // Update the patient again using the now outdated initial entity
        entity.setFirstname("2-" + entity.getFirstname());

        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Patient.class);

        // Get the patient again
        Patient entityUpdatedAgain = lookupEntityByUsername(username);

        // Verify Rest response of the updated patient, i.e. verify that the second stale update did not succeed
        assertEquals(username, entityUpdatedAgain.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), entityUpdatedAgain.getFirstname());
        assertEquals(1, (int) entityUpdatedAgain.getVersion());
    }

    @Test
    public void testDeletePatient() {
        String username = getUsername(MIN_NO);

        // Get the patient
        Patient entity = lookupEntityByUsername(username);

        // Delete the patient
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(baseUrl + "/" + entity.getId());

        // Verify state in db
        assertEquals(NO_OF_ENTITIES - 1, repository.count());

        // Get the patient again, should return an empty list
        ResponseEntity<Patient[]> entityRemoved = restTemplate.getForEntity(baseUrl + "?username=" + username, Patient[].class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entityRemoved.getStatusCode());
        assertEquals(0, entityRemoved.getBody().length);
    }

    @Test
    public void testDeleteNotExistingPatient() {

        String idNotExisting = "NON-EXISTING-ID";

        // Verify state in db
        assertEquals(NO_OF_ENTITIES, repository.count());

        // Try to delete the non-existing patient
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(baseUrl + "/" + idNotExisting);

        // Verify state in db, i.e. no change
        assertEquals(NO_OF_ENTITIES, repository.count());
    }

    private Patient lookupEntityByUsername(String username) {

        ResponseEntity<Patient[]> entities = restTemplate.getForEntity(baseUrl + "?username=" + username, Patient[].class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entities.getStatusCode());
        assertEquals(1, entities.getBody().length);

        Patient entity = entities.getBody()[0];
        assertEquals(username, entity.getUsername());
        assertNotNull(entity.getId());

        return entity;
    }

    private String getExpectedFirstname(String username) {
        return "F1";
    }

    private String getUsername(int i) {
        return "U" + i;
    }

    private JpaPatient createTestDbPatient(String username) {
        return new JpaPatient(username, "1234", "F1", "L1", 100, 200);
    }

    private Patient createTestApiEntity(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }

}