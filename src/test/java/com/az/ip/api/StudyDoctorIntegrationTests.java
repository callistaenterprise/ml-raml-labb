package com.az.ip.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Study;
import com.az.ip.api.persistence.jpa.DoctorEntity;
import com.az.ip.api.persistence.jpa.DoctorRepository;
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
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by magnus on 31/07/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest({"server.port=0", "management.port=0"})
public class StudyDoctorIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(StudyIntegrationTests.class);
    private static final String BASE_URI = "/api";
    private static final String BASE_URI_STUDIES = "/studies";
    private static final String BASE_URI_DOCTORS = "/doctors";
    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

    @Inject
    StudyRepository studyRepository;

    @Inject
    DoctorRepository doctorRepository;

    private RestTemplate restTemplate = null;
    private String baseUrlStudies = null;
    private String baseUrldoctors = null;

    @BeforeClass
    public static void setupSSL() {
        SSLUtil.registerKeyStore("server.jks");
    }

    @Before
    public void setupDb() {
        studyRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Before
    public void setupBaseUrlAndRestTemplate() {
        baseUrlStudies = PROTOCOL + "://localhost:" + port + BASE_URI + BASE_URI_STUDIES;
        baseUrldoctors = PROTOCOL + "://localhost:" + port + BASE_URI + BASE_URI_DOCTORS;
        restTemplate = new TestRestTemplate(user, pwd);
    }

    @Test
    public void testStudyDoctorPersistensLayer() {

        String studyName = "S-1";
        String doctorUsername = "D-1";

        StudyEntity study = createTestDbStudyEntity(studyName);
        DoctorEntity doctor = createTestDbDoctorEntity(doctorUsername);

        doctorRepository.save(doctor);

        study.getAssigendDoctors().add(doctor);
        studyRepository.save(study);

        DoctorEntity doctor2 = doctorRepository.findByUsername(doctorUsername);

        assertEquals(1, doctor2.getAssigendInStudies().size());
    }


    @Test
    public void testAddDoctorToStudyAPI() {

        String studyName = "S-2";
        String doctorUsername = "D-2";

        StudyEntity study = createTestDbStudyEntity(studyName);
        DoctorEntity doctor = createTestDbDoctorEntity(doctorUsername);

        ResponseEntity<Study> studyEntity = restTemplate.postForEntity(baseUrlStudies, study, Study.class);
        ResponseEntity<Doctor> doctorEntity = restTemplate.postForEntity(baseUrldoctors, doctor, Doctor.class);

        // Verify Rest response
        assertEquals(HttpStatus.OK, studyEntity.getStatusCode());
        assertNotNull(studyEntity.getBody());

        assertEquals(HttpStatus.OK, doctorEntity.getStatusCode());
        assertNotNull(doctorEntity.getBody());

        // Verify state in db
        assertEquals(1, studyRepository.count());
        assertEquals(1, doctorRepository.count());
        assertEquals(0, studyRepository.findByName(studyName).getAssigendDoctors().size());
        assertEquals(0, doctorRepository.findByUsername(doctorUsername).getAssigendInStudies().size());


        // Assign doctor to the study
        Id id = new Id().withId(doctorEntity.getBody().getId());
        String assignedDoctorsUrl = baseUrlStudies + "/" + studyEntity.getBody().getId() + "/assignedDoctors";
        ResponseEntity<String> result = restTemplate.postForEntity(assignedDoctorsUrl, id, String.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNull(result.getBody());

        // Verify db result
        assertEquals(1, studyRepository.findByName(studyName).getAssigendDoctors().size());
        assertEquals(1, doctorRepository.findByUsername(doctorUsername).getAssigendInStudies().size());

        // Assert one doctor assigned to the studie
        ResponseEntity<Id[]> listAssignedDoctors = restTemplate.getForEntity(assignedDoctorsUrl, Id[].class);

        assertEquals(HttpStatus.OK, listAssignedDoctors.getStatusCode());
        assertEquals(1, listAssignedDoctors.getBody().length);

        // Assert one studie assigned to the doctor
        String assignedInStudiesUrl = baseUrldoctors + "/" + doctorEntity.getBody().getId() + "/assignedInStudies";
        ResponseEntity<Id[]> listAssignedStudies = restTemplate.getForEntity(assignedInStudiesUrl, Id[].class);
        assertEquals(HttpStatus.OK, listAssignedStudies.getStatusCode());
        assertEquals(1, listAssignedStudies.getBody().length);

        // Remove the doctors assignment in the study
        // TODO: How do we get error http codes from a HTTP DELETE using the restTemplate???
        restTemplate.delete(assignedDoctorsUrl + "/" + doctorEntity.getBody().getId());

        // Assert that no doctor is assigned to the study anymore
        ResponseEntity<Id[]> listAssignedDoctorsAfterDelete = restTemplate.getForEntity(assignedDoctorsUrl, Id[].class);

        assertEquals(HttpStatus.OK, listAssignedDoctorsAfterDelete.getStatusCode());
        assertEquals(0, listAssignedDoctorsAfterDelete.getBody().length);

        // Assert that no studie is assigned to the doctor anymore
        ResponseEntity<Id[]> listAssignedStudiesAfterDelete = restTemplate.getForEntity(assignedInStudiesUrl, Id[].class);
        assertEquals(HttpStatus.OK, listAssignedStudiesAfterDelete.getStatusCode());
        assertEquals(0, listAssignedStudiesAfterDelete.getBody().length);
    }

    private StudyEntity createTestDbStudyEntity(String name) {
        return new StudyEntity(name, "description", new Date(), new Date());
    }

    private DoctorEntity createTestDbDoctorEntity(String username) {
        return new DoctorEntity(username, "F1", "L1");
    }

}
