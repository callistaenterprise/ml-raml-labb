package com.az.ip.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Error;
import com.az.ip.api.persistence.jpa.DoctorRepository;
import com.az.ip.api.persistence.jpa.JpaDoctor;
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
public class DoctorIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorIntegrationTests.class);
    private static final String BASE_URI = "/api/doctors";
    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

    @Inject
    DoctorRepository repository;

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
            repository.save(createTestDbEntity(getUsername(i)));
        }
        LOG.info("Created {} test doctors", repository.count());
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
    public void testPostDoctor() {

        String username = getUsername(MAX_NO + 1);

        Doctor newEntity = createTestApiEntity(username);
        ResponseEntity<Doctor> entity = restTemplate.postForEntity(baseUrl, newEntity, Doctor.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());

        // Verify the returned new entity
        assertNotNull(entity.getBody().getId());
        assertEquals(0,   (int)entity.getBody().getVersion());
        assertEquals(username, entity.getBody().getUsername());

        // Verify state in db
        assertEquals(NO_OF_ENTITIES + 1, repository.count());
    }

    @Test
    public void testPostDoctorDuplicateError() {

        Doctor newEntity = createTestApiEntity(getUsername(MIN_NO));
        ResponseEntity<Error> entity = restTemplate.postForEntity(baseUrl, newEntity, Error.class);

        // Verify Rest response
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertNotNull(entity.getBody());
        // TODO: Add verification of the fields content in the Error-object

        // Verify state in db, i.e. no new entity in the database
        assertEquals(NO_OF_ENTITIES, repository.count());
    }

    @Test
    public void testGetDoctors() {

        // Ask for all doctors, e.g. set size to -1
        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1", Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_ENTITIES, body.length);

        // Verify that we got doctors with the expected usernames, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MIN_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndIncrement()), e.getUsername()));

    }

    @Test
    public void testGetDoctorsDescending() {

        // Ask for all doctors in descending order, e.g. set size to -1
        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1&orderBy=username&order=desc", Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_ENTITIES, body.length);

        // Verify that we got doctors with the expected usernames, i.e. starting with MAX_NO and in descending order
        final AtomicInteger id = new AtomicInteger(MAX_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndDecrement()), e.getUsername()));

    }

    @Test
    public void testGetDoctorsFirstPage() {

        final int SIZE = 3;

        // TODO: Add sort order on ascending username
        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl + "?size=" + SIZE, Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first doctors as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got doctors with the expected username's, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MIN_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndIncrement()), e.getUsername()));
    }

    @Test
    public void testGetDoctorsLastPage() {

        final int SIZE = 3;

        // TODO: Add sort order on descending username
        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl + "?size=" + SIZE + "&orderBy=username&order=desc", Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first doctors as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got doctors with the expected username's, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MAX_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndDecrement()), e.getUsername()));
    }

    @Test
    public void testGetDoctorsWithPaging() {

        final int PAGE = 2;
        final int SIZE = 5;

        // TODO: Add sort order on ascending username
        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl + "?page=" + PAGE + "&size=" + SIZE, Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first doctors as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got doctors with the expected username's, i.e. starting with MIN_NO plus the offset given by PAGE and SIZE (skipping PAGE*SIZE doctors)
        final AtomicInteger id = new AtomicInteger(MIN_NO + PAGE*SIZE);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndIncrement()), e.getUsername()));
    }

    @Test
    public void testGetDoctorsWithPagingDescending() {

        final int PAGE = 2;
        final int SIZE = 5;

        // TODO: Add sort order on ascending username
        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl + "?page=" + PAGE + "&size=" + SIZE + "&orderBy=username&order=desc", Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first doctors as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got doctors with the expected username's, i.e. starting with MAX_NO minus the offset given by PAGE and SIZE (skipping PAGE*SIZE doctors)
        final AtomicInteger id = new AtomicInteger(MAX_NO - PAGE*SIZE);
        Arrays.stream(body).forEach(e -> assertEquals(getUsername(id.getAndDecrement()), e.getUsername()));
    }

    @Test
    public void testGetDoctorsNoFound() {

        repository.deleteAll();

        ResponseEntity<Doctor[]> entity = restTemplate.getForEntity(baseUrl, Doctor[].class);
        Doctor[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(0, body.length);
    }

    @Test
    public void testGetOneDoctorByName() {

        // The helper method implement the whole test already...
        lookupEntityByUsername(getUsername(MIN_NO));
    }

    @Test
    public void testGetOneDoctorById() {

        // First get the id of an entity with a known name
        String username = getUsername(MIN_NO);
        String id       = lookupEntityByUsername(username).getId();

        // Now, perform the actual test
        ResponseEntity<Doctor> entity = restTemplate.getForEntity(baseUrl + "/" + id, Doctor.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(username, entity.getBody().getUsername());
        assertEquals(id, entity.getBody().getId());
    }

    @Test
    public void testGetOneDoctorNotFoundError() {

        String idNotExisting = "NON-EXISTING-ID";

        ResponseEntity<String> entity = restTemplate.getForEntity(baseUrl + "/" + idNotExisting, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entity.getBody(), entity.getBody().contains("\"status\":404,\"error\":\"Not Found\",\"message\":\"Not Found\",\"path\":\"" + BASE_URI + "/" + idNotExisting + "\""));
    }

    @Test
    public void testUpdateOneDoctor() {

        String username = getUsername(MIN_NO);

        // Get the doctor
        Doctor entity = lookupEntityByUsername(username);

        // Verify Rest response
        assertEquals(username, entity.getUsername());
        assertEquals(getExpectedFirstname(username), entity.getFirstname());
        assertEquals(0, (int)entity.getVersion());

        // Update the first name
        // TODO how do I check the result of the update???
        entity.setFirstname("new-" + entity.getFirstname());
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Doctor.class);

        // Get the doctor again
        Doctor entityUpdated = lookupEntityByUsername(username);

        // Verify Rest response of the updated doctor
        assertEquals(username, entityUpdated.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), entityUpdated.getFirstname());
        assertEquals(1, (int) entityUpdated.getVersion());
    }

    @Test
    public void testUpdateOneDoctorPessimisticLockError() {

        String username = getUsername(MIN_NO);

        // Get the doctor
        Doctor entity = lookupEntityByUsername(username);

        // Verify Rest response
        assertEquals(username, entity.getUsername());
        assertEquals(getExpectedFirstname(username), entity.getFirstname());
        assertEquals(0, (int) entity.getVersion());

        // Update the first name
        entity.setFirstname("new-" + entity.getFirstname());
        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Doctor.class);

        // Get the doctor again
        Doctor entityUpdated = lookupEntityByUsername(username);

        // Verify Rest response of the updated doctor
        assertEquals(username, entityUpdated.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), entityUpdated.getFirstname());
        assertEquals(1, (int) entityUpdated.getVersion());



        // Update the doctor again using the now outdated initial entity
        entity.setFirstname("2-" + entity.getFirstname());

        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Doctor.class);

        // Get the doctor again
        Doctor entityUpdatedAgain = lookupEntityByUsername(username);

        // Verify Rest response of the updated doctor, i.e. verify that the second stale update did not succeed
        assertEquals(username, entityUpdatedAgain.getUsername());
        assertEquals("new-" + getExpectedFirstname(username), entityUpdatedAgain.getFirstname());
        assertEquals(1, (int) entityUpdatedAgain.getVersion());
    }

    @Test
    public void testDeleteDoctor() {
        String username = getUsername(MIN_NO);

        // Get the doctor
        Doctor entity = lookupEntityByUsername(username);

        // Delete the doctor
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(baseUrl + "/" + entity.getId());

        // Verify state in db
        assertEquals(NO_OF_ENTITIES - 1, repository.count());

        // Get the doctor again, should return an empty list
        ResponseEntity<Doctor[]> entityRemoved = restTemplate.getForEntity(baseUrl + "?username=" + username, Doctor[].class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entityRemoved.getStatusCode());
        assertEquals(0, entityRemoved.getBody().length);
    }

    @Test
    public void testDeleteNotExistingDoctor() {

        String idNotExisting = "NON-EXISTING-ID";

        // Verify state in db
        assertEquals(NO_OF_ENTITIES, repository.count());

        // Try to delete the non-existing doctor
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(baseUrl + "/" + idNotExisting);

        // Verify state in db, i.e. no change
        assertEquals(NO_OF_ENTITIES, repository.count());
    }

    private Doctor lookupEntityByUsername(String username) {

        ResponseEntity<Doctor[]> entities = restTemplate.getForEntity(baseUrl + "?username=" + username, Doctor[].class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entities.getStatusCode());
        assertEquals(1, entities.getBody().length);

        Doctor entity = entities.getBody()[0];
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

    private JpaDoctor createTestDbEntity(String username) {
        return new JpaDoctor(username, "F1", "L1");
    }

    private Doctor createTestApiEntity(String username) {
        return new Doctor().withUsername(username).withFirstname("F1").withLastname("L1");
    }

}