package com.az.ip.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Study;
import com.az.ip.api.persistence.jpa.DoctorEntity;
import com.az.ip.api.persistence.jpa.DoctorRepository;
import com.az.ip.api.persistence.jpa.StudyEntity;
import com.az.ip.api.persistence.jpa.StudyRepository;
import org.junit.*;
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
    @After
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
    public void testStudyDoctorPersistensLayerSingle() {

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
    public void testStudyDoctorPersistensLayerMulti() {

        String study1Name = "S-1";
        String study2Name = "S-2";
        String study3Name = "S-3";
        String doctor1Username = "D-1";
        String doctor2Username = "D-2";
        String doctor3Username = "D-3";

        StudyEntity study1 = createTestDbStudyEntity(study1Name);
        StudyEntity study2 = createTestDbStudyEntity(study2Name);
        StudyEntity study3 = createTestDbStudyEntity(study3Name);

        DoctorEntity doctor1 = createTestDbDoctorEntity(doctor1Username);
        DoctorEntity doctor2 = createTestDbDoctorEntity(doctor2Username);
        DoctorEntity doctor3 = createTestDbDoctorEntity(doctor3Username);

        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        doctorRepository.save(doctor3);

        // Assign no doctors to study1, one to study2 and two to study3
        study2.getAssigendDoctors().add(doctor2);
        study3.getAssigendDoctors().add(doctor2);
        study3.getAssigendDoctors().add(doctor3);

        studyRepository.save(study1);
        studyRepository.save(study2);
        studyRepository.save(study3);

        // Reread the entities from the database
        study1 = studyRepository.findByName(study1Name);
        study2 = studyRepository.findByName(study2Name);
        study3 = studyRepository.findByName(study3Name);

        doctor1 = doctorRepository.findByUsername(doctor1Username);
        doctor2 = doctorRepository.findByUsername(doctor2Username);
        doctor3 = doctorRepository.findByUsername(doctor3Username);

        // Verify the expected result...
        assertEquals(0, study1.getAssigendDoctors().size());
        assertEquals(1, study2.getAssigendDoctors().size());
        assertEquals(2, study3.getAssigendDoctors().size());

        assertTrue(study2.getAssigendDoctors().contains(doctor2));
        assertTrue(study3.getAssigendDoctors().contains(doctor2));
        assertTrue(study3.getAssigendDoctors().contains(doctor3));

        assertEquals(0, doctor1.getAssigendInStudies().size());
        assertEquals(2, doctor2.getAssigendInStudies().size());
        assertEquals(1, doctor3.getAssigendInStudies().size());

        assertTrue(doctor2.getAssigendInStudies().contains(study2));
        assertTrue(doctor2.getAssigendInStudies().contains(study3));
        assertTrue(doctor3.getAssigendInStudies().contains(study3));

    }

    // FIXME
    @Ignore
    @Test
    public void testStudyDoctorPersistensLayerDuplicateError() {
    }


    /**
     * Is this test needed or can it be replaced by the test testAddPatienToStudyAPI in PatientDoctorStudyIntegrationTests.java?
     */
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

        // Assert one study assigned to the doctor
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

        // Assert that no study is assigned to the doctor anymore
        ResponseEntity<Id[]> listAssignedStudiesAfterDelete = restTemplate.getForEntity(assignedInStudiesUrl, Id[].class);
        assertEquals(HttpStatus.OK, listAssignedStudiesAfterDelete.getStatusCode());
        assertEquals(0, listAssignedStudiesAfterDelete.getBody().length);
    }

    // FIXME
    @Ignore
    @Test
    public void testStudyDoctorAPI_negativeTests() {
    }

    private StudyEntity createTestDbStudyEntity(String name) {
        return new StudyEntity(name, "description", new Date(), new Date());
    }

    private DoctorEntity createTestDbDoctorEntity(String username) {
        return new DoctorEntity(username, "F1", "L1");
    }

}
