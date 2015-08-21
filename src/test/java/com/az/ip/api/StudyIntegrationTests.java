package com.az.ip.api;

import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Study;
import com.az.ip.api.persistence.jpa.StudyEntity;
import com.az.ip.api.persistence.jpa.StudyRepository;
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
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class StudyIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(StudyIntegrationTests.class);
    private static final String BASE_URI = "/raml-api/studies";
    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

    @Inject
    StudyRepository repository;

	private RestTemplate restTemplate = null;
    private String baseUrl = null;

    private final static int MIN_NO = 11;
    private final static int MAX_NO = 30;
    private final static int NO_OF_ENTITIES = MAX_NO - MIN_NO + 1;

    @Before
    public void setupDb() {
        repository.deleteAll();

        // Insert NO_OF_ENTITIES entities in the database
        for (int i = MIN_NO; i <= MAX_NO; i++) {
            repository.save(createTestDbEntity(getName(i)));
        }
        LOG.info("Created {} test Studies", repository.count());
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
    public void testPostStudy() {

        String name = getName((MAX_NO + 1));

        Study newEntity = createTestApiEntity(name);
        ResponseEntity<Study> entity = restTemplate.postForEntity(baseUrl, newEntity, Study.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNotNull(entity.getBody());

        // Verify the returned new entity
        assertNotNull(entity.getBody().getId());
        assertEquals(0, (int)entity.getBody().getVersion());
        assertEquals(name,   entity.getBody().getName());

        // Verify state in db
        assertEquals(NO_OF_ENTITIES + 1, repository.count());
    }

    @Test
    public void testPostStudyDuplicateError() {

        Study newEntity = createTestApiEntity(getName(MIN_NO));
        ResponseEntity<Error> entity = restTemplate.postForEntity(baseUrl, newEntity, Error.class);

        // Verify Rest response
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertNotNull(entity.getBody());
        // TODO: Add verification of the fields content in the Error-object

        // Verify state in db, i.e. no new entity in the database
        assertEquals(NO_OF_ENTITIES, repository.count());
    }

    @Test
    public void testGetStudies() {

        // Ask for all Studies, e.g. set size to -1
        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1", Study[].class);
        Study[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_ENTITIES, body.length);

        // Verify that we got studies with the expected names, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MIN_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getName(id.getAndIncrement()), e.getName()));

    }

    @Test
    public void testGetStudiesDescending() {

        // Ask for all studies in descending order, e.g. set size to -1
        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl + "?size=-1&orderBy=name&order=desc", Study[].class);
        Study[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(NO_OF_ENTITIES, body.length);

        // Verify that we got Studies with the expected names, i.e. starting with MAX_NO and in descending order
        final AtomicInteger id = new AtomicInteger(MAX_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getName(id.getAndDecrement()), e.getName()));

    }

    @Test
    public void testGetStudiesFirstPage() {

        final int SIZE = 3;

        // TODO: Add sort order on ascending name
        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl + "?size=" + SIZE, Study[].class);
        Study[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first Studies as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got Studies with the expected name's, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MIN_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getName(id.getAndIncrement()), e.getName()));
    }

    @Test
    public void testGetStudiesLastPage() {

        final int SIZE = 3;

        // TODO: Add sort order on descending name
        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl + "?size=" + SIZE + "&orderBy=name&order=desc", Study[].class);
        Study[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first Studies as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got Studies with the expected name's, i.e. starting with MIN_NO and in ascending order
        final AtomicInteger id = new AtomicInteger(MAX_NO);
        Arrays.stream(body).forEach(e -> assertEquals(getName(id.getAndDecrement()), e.getName()));
    }

    @Test
    public void testGetStudiesWithPaging() {

        final int PAGE = 2;
        final int SIZE = 5;

        // TODO: Add sort order on ascending name
        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl + "?page=" + PAGE + "&size=" + SIZE, Study[].class);
        Study[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first Studies as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got Studies with the expected name's, i.e. starting with MIN_NO plus the offset given by PAGE and SIZE (skipping PAGE*SIZE Studies)
        final AtomicInteger id = new AtomicInteger(MIN_NO + PAGE*SIZE);
        Arrays.stream(body).forEach(e -> assertEquals(getName(id.getAndIncrement()), e.getName()));
    }

    @Test
    public void testGetStudiesWithPagingDescending() {

        final int PAGE = 2;
        final int SIZE = 5;

        // TODO: Add sort order on ascending name
        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl + "?page=" + PAGE + "&size=" + SIZE + "&orderBy=name&order=desc", Study[].class);
        Study[] body = entity.getBody();

        // Verify the response
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        // Verify that we only got the first Studies as specified by LIMIT
        assertEquals(SIZE, body.length);

        // Verify that we got Studies with the expected name's, i.e. starting with MAX_NO minus the offset given by PAGE and SIZE (skipping PAGE*SIZE Studies)
        final AtomicInteger id = new AtomicInteger(MAX_NO - PAGE*SIZE);
        Arrays.stream(body).forEach(p -> assertEquals(getName(id.getAndDecrement()), p.getName()));
    }

    @Test
    public void testGetStudiesNoFound() {

        repository.deleteAll();

        ResponseEntity<Study[]> entity = restTemplate.getForEntity(baseUrl, Study[].class);
        Study[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(0, body.length);
    }

    @Test
    public void testGetOneStudyByName() {

        // The helper method implement the whole test already...
        lookupEntityByName(getName(MIN_NO));
    }

    @Test
    public void testGetOneStudyById() {

        // First get the id of an entity with a known name
        String name = getName(MIN_NO);
        String id   = lookupEntityByName(name).getId();

        // Now, perform the actual test
        ResponseEntity<Study> entity = restTemplate.getForEntity(baseUrl + "/" + id, Study.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(name, entity.getBody().getName());
        assertEquals(id, entity.getBody().getId());
    }

    @Test
    public void testGetOneStudyNotFoundError() {

        String idNotExisting = "NON-EXISTING-ID";

        ResponseEntity<String> entity = restTemplate.getForEntity(baseUrl + "/" + idNotExisting, String.class);

        // Verify Rest response
        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        // TODO: Improve quality of this test, e.g. parse the json response...
        assertTrue("Unexpected error message: " + entity.getBody(), entity.getBody().contains("\"status\":404,\"error\":\"Not Found\",\"message\":\"Not Found\",\"path\":\"" + BASE_URI + "/" + idNotExisting + "\""));
    }


    @Test
    public void testUpdateOneStudy() {

        String name = getName(MIN_NO);

        // Get the study
        Study entity = lookupEntityByName(name);

        // Verify Rest response
        assertEquals(name, entity.getName());
        assertEquals(getExpectedDescription(name), entity.getDescription());
        assertEquals(0, (int) entity.getVersion());

        // Update the first name
        // TODO how do I check the result of the update???
        entity.setDescription("new-" + entity.getDescription());
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Study.class);

        // Get the Study again
        Study entityUpdated = lookupEntityByName(name);

        // Verify Rest response of the updated study
        assertEquals(name, entityUpdated.getName());
        assertEquals("new-" + getExpectedDescription(name), entityUpdated.getDescription());
        assertEquals(1, (int) entityUpdated.getVersion());
    }

    @Test
    public void testUpdateOneStudyPessimisticLockError() {

        String name = getName(MIN_NO);

        // Get the study
        Study entity = lookupEntityByName(name);

        // Verify Rest response
        assertEquals(name, entity.getName());
        assertEquals(getExpectedDescription(name), entity.getDescription());
        assertEquals(0, (int)entity.getVersion());

        // Update the first name
        entity.setDescription("new-" + entity.getDescription());
        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Study.class);

        // Get the Study again
        Study entityUpdated = lookupEntityByName(name);

        // Verify Rest response of the updated Study
        assertEquals(name, entityUpdated.getName());
        assertEquals("new-" + getExpectedDescription(name), entityUpdated.getDescription());
        assertEquals(1, (int)entityUpdated.getVersion());



        // Update the Study again using the now outdated initial entity
        entity.setDescription("2-" + entity.getDescription());

        // TODO: How do we get error http codes from a HTTP PUT using the restTemplate???
        restTemplate.put(baseUrl + "/" + entity.getId(), entity, Study.class);

        // Get the Study again
        Study entityUpdatedAgain = lookupEntityByName(name);

        // Verify Rest response of the updated Study, i.e. verify that the second stale update did not succeed
        assertEquals(name, entityUpdatedAgain.getName());
        assertEquals("new-" + getExpectedDescription(name), entityUpdatedAgain.getDescription());
        assertEquals(1, (int)entityUpdatedAgain.getVersion());
    }


    @Test
    public void testDeleteStudy() {
        String name = getName(MIN_NO);

        // Get the Study
        Study entity = lookupEntityByName(name);

        // Delete the Study
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(baseUrl + "/" + entity.getId());

        // Verify state in db
        assertEquals(NO_OF_ENTITIES - 1, repository.count());

        // Get the Study again, should return an empty list
        ResponseEntity<Study[]> entityRemoved = restTemplate.getForEntity(baseUrl + "?name=" + name, Study[].class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entityRemoved.getStatusCode());
        assertEquals(0, entityRemoved.getBody().length);
    }

    @Test
    public void testDeleteNotExistingStudy() {

        String idNotExisting = "NON-EXISTING-ID";

        // Verify state in db
        assertEquals(NO_OF_ENTITIES, repository.count());

        // Try to delete the non-existing Study
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(baseUrl + "/" + idNotExisting);

        // Verify state in db, i.e. no change
        assertEquals(NO_OF_ENTITIES, repository.count());
    }

    private Study lookupEntityByName(String name) {

        ResponseEntity<Study[]> entities = restTemplate.getForEntity(baseUrl + "?name=" + name, Study[].class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entities.getStatusCode());
        assertEquals(1, entities.getBody().length);

        Study entity = entities.getBody()[0];
        assertEquals(name, entity.getName());
        assertNotNull(entity.getId());

        return entity;
    }

    private String getExpectedDescription(String name) {
        return "description";
    }

    private String getName(int i) {
        return "Study-" + i;
    }

    private StudyEntity createTestDbEntity(String name) {
        return new StudyEntity(name, getExpectedDescription(name), new Date(), new Date());
    }

    private Study createTestApiEntity(String name) {
        return new Study().withName(name).withDescription(getExpectedDescription(name)).withStartdate(new Date()).withEnddate(new Date());
    }

}
