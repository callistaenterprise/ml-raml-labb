package com.az.ip.api;

import com.az.ip.api.model.Patient;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class ApplicationTests {

    @Value("${local.server.port}")
    int port;

	private RestTemplate restTemplate = new TestRestTemplate();
    private String baseUrl = null;

    @Before
    public void setupBaseUrl() {
        baseUrl = "http://localhost:" + port + "/patients";
    }

    @Test
    public void testPostPatient() {

        String username = "U1";

        Patient newPatient = createTestPatient(username);
        ResponseEntity entity = restTemplate.postForEntity(baseUrl, newPatient, Patient.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertNull(entity.getBody());
    }

    @Test
    public void testGetPatient() {

        String username = "U1";

        ResponseEntity<Patient> entity = restTemplate.getForEntity(baseUrl + "/" + username, Patient.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(username, entity.getBody().getUsername());
    }

    @Test
    public void testGetPatientsOk() {
        performGetPatientsTest("", HttpStatus.OK);
    }

    private void performGetPatientsTest(String suffix, HttpStatus httpStatus) {

        ResponseEntity<Patient[]> entity = restTemplate.getForEntity(baseUrl + suffix, Patient[].class);
        Patient[] body = entity.getBody();

        assertEquals(httpStatus, entity.getStatusCode());
        assertEquals(3, body.length);
        assertEquals("U1", body[0].getUsername());
        assertEquals("U2", body[1].getUsername());
        assertEquals("U3", body[2].getUsername());
    }

    private Patient createTestPatient(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }

}