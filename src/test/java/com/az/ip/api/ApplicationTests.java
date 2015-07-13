package com.az.ip.api;

import com.az.ip.api.model.Patient;
import com.az.ip.api.persistence.jpa.PatientRepository;
import org.junit.Before;
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
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class ApplicationTests {

    @Value("${local.server.port}")
    int port;

    @Inject
    PatientRepository repository;

	private RestTemplate restTemplate = new TestRestTemplate();
    private String baseUrl = null;

    @Before
    public void setupDb() {
        repository.deleteAll();
        repository.save(createDbPatient("U11"));
        repository.save(createDbPatient("U21"));
        repository.save(createDbPatient("U31"));
    }

    @Before
    public void setupBaseUrl() {
        baseUrl = "http://localhost:" + port + "/patients";
    }

    @Test
    public void testPostPatient() {

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
    public void testGetPatient() {

        String username = "U21";

        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(username, entity.getBody().getUsername());
    }

    @Test
    public void testGetPatientsOk() {

        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl, Patient[].class);
        Patient[] body = entity.getBody();

        // Verify Rest response
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(3, body.length);
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
}