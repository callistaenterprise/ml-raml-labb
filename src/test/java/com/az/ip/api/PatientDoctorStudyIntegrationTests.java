package com.az.ip.api;

import com.az.ip.api.gen.model.*;
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
import java.util.Arrays;
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
    private static final String BASE_URI = "/raml-api";
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
    MeasurementRepository measurementRepository;

    @Inject
    PatientDoctorStudyRepository pdsRepository;

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
        measurementRepository.deleteAll();
        pdsRepository.deleteAll();
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
        pdsRepository.save(relationEntity);

        // Reread the entities from the database
        patient = patientRepository.findByUsername(patientUsername);
        doctor = doctorRepository.findByUsername(doctorUsername);
        study = studyRepository.findByName(studyName);

        // Verify the expected result...

        assertEquals(1, pdsRepository.findByPatient(patient).size());
        assertEquals(1, pdsRepository.findByDoctor(doctor).size());
        assertEquals(1, pdsRepository.findByStudy(study).size());
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
        pdsRepository.save(new PatientDoctorStudyEntity(patient1, doctor1, study1));
        pdsRepository.save(new PatientDoctorStudyEntity(patient1, doctor2, study2));
        pdsRepository.save(new PatientDoctorStudyEntity(patient2, doctor1, study1));
        pdsRepository.save(new PatientDoctorStudyEntity(patient3, doctor2, study2));
        pdsRepository.save(new PatientDoctorStudyEntity(patient4, doctor3, study2));

        // Verify with repository-find methods...
        assertEquals(1, pdsRepository.findByPatientAndDoctorAndStudy(patient1, doctor1, study1).size());
        assertEquals(0, pdsRepository.findByPatientAndDoctorAndStudy(patient2, doctor3, study2).size());

        assertEquals(0, pdsRepository.findByStudyAndDoctor(study1, doctor3).size());
        assertEquals(2, pdsRepository.findByStudyAndDoctor(study1, doctor1).size());
        assertEquals(2, pdsRepository.findByStudyAndDoctor(study2, doctor2).size());
        assertEquals(1, pdsRepository.findByStudyAndDoctor(study2, doctor3).size());

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
        assertEquals(2, pdsRepository.findByPatient(patient1).size());
        assertEquals(1, pdsRepository.findByPatient(patient2).size());
        assertEquals(1, pdsRepository.findByPatient(patient3).size());
        assertEquals(1, pdsRepository.findByPatient(patient4).size());

        assertEquals(2, pdsRepository.findByDoctor(doctor1).size());
        assertEquals(2, pdsRepository.findByDoctor(doctor2).size());
        assertEquals(1, pdsRepository.findByDoctor(doctor3).size());

        assertEquals(2, pdsRepository.findByStudy(study1).size());
        assertEquals(3, pdsRepository.findByStudy(study2).size());
    }

    @Test
    public void testPatientDoctorStudyPersistensLayerMulti2() {

        String patient1Username = "P-1";
        String patient2Username = "P-2";

        String doctor1Username = "D-1";
        String doctor2Username = "D-2";

        String study1Name = "S-1";

        PatientEntity patient1 = createTestDbPatientEntity(patient1Username);
        PatientEntity patient2 = createTestDbPatientEntity(patient2Username);

        DoctorEntity doctor1 = createTestDbDoctorEntity(doctor1Username);
        DoctorEntity doctor2 = createTestDbDoctorEntity(doctor2Username);

        StudyEntity study1 = createTestDbStudyEntity(study1Name);

        patientRepository.save(patient1);
        patientRepository.save(patient2);

        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);

        studyRepository.save(study1);

        // Assign doctors to the study
        study1.getAssigendDoctors().add(doctor1);
        study1.getAssigendDoctors().add(doctor2);

        studyRepository.save(study1);

        // Assign patients to studies via doctor1
        pdsRepository.save(new PatientDoctorStudyEntity(patient1, doctor1, study1));
        pdsRepository.save(new PatientDoctorStudyEntity(patient2, doctor1, study1));

        assertEquals(2, pdsRepository.count());
        assertEquals(1, pdsRepository.findByPatient(patient1).size());
        assertEquals(1, pdsRepository.findByPatientId(patient1.getId()).size());
    }

    // FIXME
    @Ignore
    @Test
    public void testPatientDoctorStudyPersistensLayerMeasurement() {
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
        relationEntity.getMeasurements().add(createTestDbMeasurementEntity(relationEntity, 200));
        relationEntity.getMeasurements().add(createTestDbMeasurementEntity(relationEntity, 300));

        pdsRepository.save(relationEntity);

        fail("Not done here...");
    }

    // FIXME
    @Ignore
    @Test
    public void testPatientDoctorStudyPersistensLayerDuplicateError() {
    }


    @Test
    public void testAddPatientToStudyAPI() {

        String patientUsername = "P-2";
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
        ResponseEntity response2 = restTemplate.postForEntity(assignedInStudiesUrl + "/" + studyId + "/patients", new Id().withId(patientId), String.class);

        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertNull(response2.getBody());

        // Assert that one patient is assigned to the study by the doctor
        ResponseEntity<Id[]> listAssignedPatients = restTemplate.getForEntity(assignedInStudiesUrl + "/" + studyId + "/patients", Id[].class);

        assertEquals(HttpStatus.OK, listAssignedPatients.getStatusCode());
        assertEquals(1, listAssignedPatients.getBody().length);
        assertEquals(patientId, listAssignedPatients.getBody()[0].getId());



        // 4. Get the patient, lookup the study and add some measurements
        ResponseEntity<Patient[]> patients = restTemplate.getForEntity(baseUrlPatients + "?username=" + patientUsername, Patient[].class);

        assertEquals(HttpStatus.OK, patients.getStatusCode());
        assertEquals(1, patients.getBody().length);

        patient = patients.getBody()[0];
        assertEquals(patientUsername, patient.getUsername());

        // ...get the patients studies...
        ResponseEntity<Id[]> studyIds = restTemplate.getForEntity(baseUrlPatients + "/" + patient.getId() + "/studies", Id[].class);
        assertEquals(HttpStatus.OK, studyIds.getStatusCode());

        // ...lookup the study...
        Study theStudy = Arrays.stream(studyIds.getBody())
            .map(sId -> getStudy(sId))
            .filter(s -> s.getName().equals(studyName))
            .findFirst().get();

        // ...ok, we found the study, let's add some measurements...
        ResponseEntity<Measurement> measurement1 = restTemplate.postForEntity(baseUrlPatients + "/" + patient.getId() + "/studies/" + theStudy.getId() + "/measurements", createTestApiMeasurementEntity(1000), Measurement.class);
        assertEquals(HttpStatus.OK, measurement1.getStatusCode());

        ResponseEntity<Measurement> measurement2 = restTemplate.postForEntity(baseUrlPatients + "/" + patient.getId() + "/studies/" + theStudy.getId() + "/measurements", createTestApiMeasurementEntity(2000), Measurement.class);
        assertEquals(HttpStatus.OK, measurement2.getStatusCode());


        // ...wrap up with verifying that we can get the two measurements again...
        ResponseEntity<Measurement[]> measurements = restTemplate.getForEntity(baseUrlPatients + "/" + patient.getId() + "/studies/" + theStudy.getId() + "/measurements", Measurement[].class);
        assertEquals(HttpStatus.OK, measurements.getStatusCode());
        assertEquals(2, measurements.getBody().length);

        assertEquals(1000, (int)measurements.getBody()[0].getSteps());
        assertEquals(2000, (int)measurements.getBody()[1].getSteps());


        // ...also ensure that we can get the measurements from the study...
        ResponseEntity<Measurement[]> measurementsFromStudy = restTemplate.getForEntity(baseUrlStudies + "/" + theStudy.getId() + "/measurements", Measurement[].class);
        assertEquals(HttpStatus.OK, measurementsFromStudy.getStatusCode());
        assertEquals(2, measurementsFromStudy.getBody().length);

        assertEquals(1000, (int)measurementsFromStudy.getBody()[0].getSteps());
        assertEquals(2000, (int)measurementsFromStudy.getBody()[1].getSteps());


        // Verify state in db
        assertEquals(1, patientRepository.count());
        assertEquals(1, doctorRepository.count());
        assertEquals(1, studyRepository.count());
        assertEquals(1, studyRepository.findByName(studyName).getAssigendDoctors().size());
        assertEquals(1, doctorRepository.findByUsername(doctorUsername).getAssigendInStudies().size());
        assertEquals(1, pdsRepository.count());
        PatientDoctorStudyEntity mappingEntity = pdsRepository.findAll().iterator().next();
        assertEquals(patientId,  mappingEntity.getPatient().getId());
        assertEquals(doctorId,   mappingEntity.getDoctor().getId());
        assertEquals(studyId,    mappingEntity.getStudy().getId());
        assertEquals(2,          mappingEntity.getMeasurements().size());


        // Verify db response
        assertEquals(1, studyRepository.findByName(studyName).getAssigendDoctors().size());
        assertEquals(1, doctorRepository.findByUsername(doctorUsername).getAssigendInStudies().size());
    }

    private Study getStudy(Id sId) {
        ResponseEntity<Study> s = restTemplate.getForEntity(baseUrlStudies + "/" + sId.getId(), Study.class);
        assertEquals(HttpStatus.OK, s.getStatusCode());
        return s.getBody();
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
        pdsRepository.save(relationEntity);

        // verify the setup
        assertEquals(1, pdsRepository.count());
        PatientDoctorStudyEntity mappingEntity = pdsRepository.findAll().iterator().next();
        assertEquals(patient.getId(), mappingEntity.getPatient().getId());
        assertEquals(doctor.getId(), mappingEntity.getDoctor().getId());
        assertEquals(study.getId(), mappingEntity.getStudy().getId());

        // Now, use the API to remove the patient from the studie
        String assignedInStudiesUrl = baseUrldoctors + "/" + doctor.getId() + "/assignedInStudies";

        // TODO: How to verify the response???
        restTemplate.delete(assignedInStudiesUrl + "/" + study.getId() + "/patients/" + patient.getId());

        // TODO: test removal of measuremments here as well?

        // Verify state in db
        assertEquals(0, pdsRepository.count());

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

    private MeasurementEntity createTestDbMeasurementEntity(PatientDoctorStudyEntity pds, int steps) {
        return new MeasurementEntity(pds, "descr", new Date(), steps);
    }

    private Measurement createTestApiMeasurementEntity(int steps) {
        return new Measurement().withDescription("descr").withTimestamp(new Date()).withSteps(steps);
    }

}
