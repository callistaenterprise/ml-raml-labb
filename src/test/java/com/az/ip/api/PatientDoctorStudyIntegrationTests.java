package com.az.ip.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Patient;
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

    // FIXME
    @Ignore
    @Test
    public void testPatientDoctorStudyPersistensLayerDuplicateError() {
    }


    @Test
    public void testAddPatienToStudyAPI() {

        String patientUsername = "D-2";
        String doctorUsername = "D-2";
        String studyName = "S-2";

        Patient patient = createTestApiPatientEntity(patientUsername);
        Study   study   = createTestApiStudyEntity(studyName);
        Doctor  doctor  = createTestApiDoctorEntity(doctorUsername);



        // 1. Setup a study
        ResponseEntity<Study> studyResponse = restTemplate.postForEntity(baseUrlStudies, study,   Study.class);

        assertEquals(HttpStatus.OK, studyResponse.getStatusCode());
        assertNotNull(studyResponse.getBody());

        String studyId = studyResponse.getBody().getId();



        // 2. Create a doctor and assign the doctor to the study
        ResponseEntity<Doctor>  doctorResponse  = restTemplate.postForEntity(baseUrldoctors, doctor,  Doctor.class);

        assertEquals(HttpStatus.OK, doctorResponse.getStatusCode());
        assertNotNull(doctorResponse.getBody());

        String doctorId = doctorResponse.getBody().getId();

        // ... now assign the doctor to the study ...
        String assignedDoctorsUrl = baseUrlStudies + "/" + studyId + "/assignedDoctors";
        ResponseEntity response1 = restTemplate.postForEntity(assignedDoctorsUrl, new Id().withId(doctorId), String.class);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertNull(response1.getBody());


        // Assert that one doctor is assigned to the study
        ResponseEntity<Id[]> listAssignedDoctors = restTemplate.getForEntity(assignedDoctorsUrl, Id[].class);

        assertEquals(HttpStatus.OK, listAssignedDoctors.getStatusCode());
        assertEquals(1, listAssignedDoctors.getBody().length);
        assertEquals(doctorId, listAssignedDoctors.getBody()[0].getId());

        // Assert that one study is assigned to the doctor
        String assignedInStudiesUrl = baseUrldoctors + "/" + doctorId + "/assignedInStudies";
        ResponseEntity<Id[]> listAssignedStudies = restTemplate.getForEntity(assignedInStudiesUrl, Id[].class);

        assertEquals(HttpStatus.OK, listAssignedStudies.getStatusCode());
        assertEquals(1, listAssignedStudies.getBody().length);
        assertEquals(studyId, listAssignedStudies.getBody()[0].getId());



        // 3. Create a patient and assign the patient to the doctor and the study
        ResponseEntity<Patient> patientResponse = restTemplate.postForEntity(baseUrlPatients, patient, Patient.class);

        assertEquals(HttpStatus.OK, patientResponse.getStatusCode());
        assertNotNull(patientResponse.getBody());

        String patientId = patientResponse.getBody().getId();

        // ... now assign the patient to doctor and the study ...
        ResponseEntity response2 = restTemplate.postForEntity(assignedInStudiesUrl + "/" + studyId, new Id().withId(patientId), String.class);

        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertNull(response2.getBody());

        // Assert that one patient is assigned to the study by the doctor
        ResponseEntity<Id[]> listAssignedPatients = restTemplate.getForEntity(assignedInStudiesUrl + "/" + studyId, Id[].class);

        assertEquals(HttpStatus.OK, listAssignedPatients.getStatusCode());
        assertEquals(1, listAssignedPatients.getBody().length);
        assertEquals(patientId, listAssignedPatients.getBody()[0].getId());



        // 1. Verify state in db
        assertEquals(1, patientRepository.count());
        assertEquals(1, doctorRepository.count());
        assertEquals(1, studyRepository.count());
        assertEquals(1, studyRepository.findByName(studyName).getAssigendDoctors().size());
        assertEquals(1, doctorRepository.findByUsername(doctorUsername).getAssigendInStudies().size());
        assertEquals(1, patientDoctorStudyRepository.count());
        PatientDoctorStudyEntity mappingEntity = patientDoctorStudyRepository.findAll().iterator().next();
        assertEquals(patientId,  mappingEntity.getPatient().getId());
        assertEquals(doctorId,   mappingEntity.getDoctor().getId());
        assertEquals(studyId,    mappingEntity.getStudy().getId());



        // Verify db response
        assertEquals(1, studyRepository.findByName(studyName).getAssigendDoctors().size());
        assertEquals(1, doctorRepository.findByUsername(doctorUsername).getAssigendInStudies().size());
    }


    @Test
    public void testDeletePatientFromStudyAPI() {

        // Prepare the database for one patient assigned to one study by one doctor
        PatientEntity patient = createTestDbPatientEntity("P-1");
        DoctorEntity  doctor = createTestDbDoctorEntity("D-1");
        StudyEntity   study = createTestDbStudyEntity("S-1");

        patientRepository.save(patient);
        doctorRepository.save(doctor);
        studyRepository.save(study);

        PatientDoctorStudyEntity relationEntity = new PatientDoctorStudyEntity(patient, doctor, study);
        patientDoctorStudyRepository.save(relationEntity);

        // verify the setup
        assertEquals(1, patientDoctorStudyRepository.count());
        PatientDoctorStudyEntity mappingEntity = patientDoctorStudyRepository.findAll().iterator().next();
        assertEquals(patient.getId(), mappingEntity.getPatient().getId());
        assertEquals(doctor.getId(), mappingEntity.getDoctor().getId());
        assertEquals(study.getId(), mappingEntity.getStudy().getId());

        // Now, use the API to remove the patient from the studie
        String assignedInStudiesUrl = baseUrldoctors + "/" + doctor.getId() + "/assignedInStudies";

        // TODO: How to verify the response???
        restTemplate.delete(assignedInStudiesUrl + "/" + study.getId() + "/" + patient.getId());


        // Verify state in db
        assertEquals(0, patientDoctorStudyRepository.count());

    }


    // FIXME
    @Ignore
    @Test
    public void testPatientDoctorStudyAPI_negativeTests() {
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

    private Patient createTestApiPatientEntity(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }

    private Doctor createTestApiDoctorEntity(String username) {
        return new Doctor().withUsername(username).withFirstname("F1").withLastname("L1");
    }

    private Study createTestApiStudyEntity(String name) {
        return new Study().withName(name).withDescription("descr").withStartdate(new Date()).withEnddate(new Date());
    }
}
