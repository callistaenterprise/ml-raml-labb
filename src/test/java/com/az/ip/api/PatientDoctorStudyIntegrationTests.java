package com.az.ip.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Study;
import com.az.ip.api.persistence.jpa.*;
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
public class PatientDoctorStudyIntegrationTests {

    private static final Logger LOG = LoggerFactory.getLogger(StudyIntegrationTests.class);
    private static final String BASE_URI = "/api";
    private static final String BASE_URI_PATIENTS = "/patients";
    private static final String BASE_URI_DOCTORS = "/doctors";
    private static final String BASE_URI_STUDIES = "/studies";
    private static final String PROTOCOL = "http";

    @Value("${local.server.port}")
    int port;

    @Value("${mysuer:demo}")
    String user;

    @Value("${mypwd:omed.1}")
    String pwd;

    @Inject
    PatientDoctorStudyRepository patientDoctorStudyRepository;

    @Inject
    PatientRepository patientRepository;

    @Inject
    StudyRepository studyRepository;

    @Inject
    DoctorRepository doctorRepository;

    private RestTemplate restTemplate = null;
    private String baseUrlPatients = null;
    private String baseUrlStudies = null;
    private String baseUrldoctors = null;

    @BeforeClass
    public static void setupSSL() {
        SSLUtil.registerKeyStore("server.jks");
    }

    @Before
    @After
    public void setupDb() {
        patientDoctorStudyRepository.deleteAll();
        patientRepository.deleteAll();
        studyRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Before
    public void setupBaseUrlAndRestTemplate() {
        baseUrlPatients = PROTOCOL + "://localhost:" + port + BASE_URI + BASE_URI_PATIENTS;
        baseUrlStudies = PROTOCOL + "://localhost:" + port + BASE_URI + BASE_URI_STUDIES;
        baseUrldoctors = PROTOCOL + "://localhost:" + port + BASE_URI + BASE_URI_DOCTORS;
        restTemplate = new TestRestTemplate(user, pwd);
    }

    @Test
    public void testPatientDoctorStudyPersistensLayerSingle() {

        String patientUsername = "P-1";
        String studyName = "S-1";
        String doctorUsername = "D-1";

        PatientEntity patient = createTestDbPatientEntity(patientUsername);
        DoctorEntity  doctor = createTestDbDoctorEntity(doctorUsername);
        StudyEntity   study = createTestDbStudyEntity(studyName);

        patientRepository.save(patient);
        doctorRepository.save(doctor);
        studyRepository.save(study);

        PatientDoctorStudyEntity relationEntity = new PatientDoctorStudyEntity(patient, doctor, study);
        patientDoctorStudyRepository.save(relationEntity);

        // Reread the entities from the database
        patient = patientRepository.findByUsername(patientUsername);
        doctor = doctorRepository.findByUsername(doctorUsername);
        study = studyRepository.findByName(studyName);

        // Verify the expected result...
        assertEquals(1, patient.getStudiesAndDoctors().size());
        assertEquals(1, doctor.getPatientsInStudies().size());
        assertEquals(1, study.getPatientsAndDoctors().size());
    }

    @Test
    public void testPatientDoctorStudyPersistensLayerMulti() {

        String patient1Username = "P-1";
        String patient2Username = "P-2";
        String patient3Username = "P-3";
        String patient4Username = "P-4";

        String doctor1Username = "D-1";
        String doctor2Username = "D-2";
        String doctor3Username = "D-3";

        String study1Name = "S-1";
        String study2Name = "S-2";

        PatientEntity patient1 = createTestDbPatientEntity(patient1Username);
        PatientEntity patient2 = createTestDbPatientEntity(patient2Username);
        PatientEntity patient3 = createTestDbPatientEntity(patient3Username);
        PatientEntity patient4 = createTestDbPatientEntity(patient4Username);

        DoctorEntity doctor1 = createTestDbDoctorEntity(doctor1Username);
        DoctorEntity doctor2 = createTestDbDoctorEntity(doctor2Username);
        DoctorEntity doctor3 = createTestDbDoctorEntity(doctor3Username);

        StudyEntity study1 = createTestDbStudyEntity(study1Name);
        StudyEntity study2 = createTestDbStudyEntity(study2Name);

        patientRepository.save(patient1);
        patientRepository.save(patient2);
        patientRepository.save(patient3);
        patientRepository.save(patient4);

        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        doctorRepository.save(doctor3);

        studyRepository.save(study1);
        studyRepository.save(study2);

        // Assign doctors and patients to studies
        patientDoctorStudyRepository.save(new PatientDoctorStudyEntity(patient1, doctor1, study1));
        patientDoctorStudyRepository.save(new PatientDoctorStudyEntity(patient1, doctor2, study2));
        patientDoctorStudyRepository.save(new PatientDoctorStudyEntity(patient2, doctor1, study1));
        patientDoctorStudyRepository.save(new PatientDoctorStudyEntity(patient3, doctor2, study2));
        patientDoctorStudyRepository.save(new PatientDoctorStudyEntity(patient4, doctor3, study2));

        // Verify with respository-find methods...
        assertEquals(1, patientDoctorStudyRepository.findByPatientAndDoctorAndStudy(patient1, doctor1, study1).size());
        assertEquals(0, patientDoctorStudyRepository.findByPatientAndDoctorAndStudy(patient2, doctor3, study2).size());

        assertEquals(0, patientDoctorStudyRepository.findByStudyAndDoctor(study1, doctor3).size());
        assertEquals(2, patientDoctorStudyRepository.findByStudyAndDoctor(study1, doctor1).size());
        assertEquals(2, patientDoctorStudyRepository.findByStudyAndDoctor(study2, doctor2).size());
        assertEquals(1, patientDoctorStudyRepository.findByStudyAndDoctor(study2, doctor3).size());

        // Reread the entities from the database
        patient1 = patientRepository.findByUsername(patient1Username);
        patient2 = patientRepository.findByUsername(patient2Username);
        patient3 = patientRepository.findByUsername(patient3Username);
        patient4 = patientRepository.findByUsername(patient4Username);

        doctor1 = doctorRepository.findByUsername(doctor1Username);
        doctor2 = doctorRepository.findByUsername(doctor2Username);
        doctor3 = doctorRepository.findByUsername(doctor3Username);

        study1 = studyRepository.findByName(study1Name);
        study2 = studyRepository.findByName(study2Name);

        // Verify the expected result...
        assertEquals(2, patient1.getStudiesAndDoctors().size());
        assertEquals(1, patient2.getStudiesAndDoctors().size());
        assertEquals(1, patient3.getStudiesAndDoctors().size());
        assertEquals(1, patient4.getStudiesAndDoctors().size());

        assertEquals(2, doctor1.getPatientsInStudies().size());
        assertEquals(2, doctor2.getPatientsInStudies().size());
        assertEquals(1, doctor3.getPatientsInStudies().size());

        assertEquals(2, study1.getPatientsAndDoctors().size());
        assertEquals(3, study2.getPatientsAndDoctors().size());
    }

    @Ignore
    @Test
    public void testPatientDoctorStudyPersistensLayerDuplicateError() {
    }


    @Ignore
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

    private PatientEntity createTestDbPatientEntity(String username) {
        return new PatientEntity(username, "1234", "F1", "L1", 100, 200);
    }

    private StudyEntity createTestDbStudyEntity(String name) {
        return new StudyEntity(name, "description", new Date(), new Date());
    }

    private DoctorEntity createTestDbDoctorEntity(String username) {
        return new DoctorEntity(username, "F1", "L1");
    }

}
