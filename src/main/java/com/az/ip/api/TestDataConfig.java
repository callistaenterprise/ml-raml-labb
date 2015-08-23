package com.az.ip.api;

import com.az.ip.api.persistence.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.inject.Inject;
import java.util.Date;

/**
 * Created by magnus on 23/08/15.
 */
@Configuration
@Profile("test")
public class TestDataConfig {

    private static final Logger LOG = LoggerFactory.getLogger(TestDataConfig.class);

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

    @Bean
    public String setupTestData() {

        LOG.info("Load some test data into the db...");

        PatientEntity patient1 = createTestDbPatientEntity("patient-1");
        PatientEntity patient2 = createTestDbPatientEntity("patient-2");
        PatientEntity patient3 = createTestDbPatientEntity("patient-3");
        PatientEntity patient4 = createTestDbPatientEntity("patient-4");

        DoctorEntity doctor1 = createTestDbDoctorEntity("doctor-1");
        DoctorEntity doctor2 = createTestDbDoctorEntity("doctor-2");
        DoctorEntity doctor3 = createTestDbDoctorEntity("doctor-3");

        StudyEntity study1 = createTestDbStudyEntity("study-1");
        StudyEntity study2 = createTestDbStudyEntity("study-2");

        study1.getAssigendDoctors().add(doctor1);
        study2.getAssigendDoctors().add(doctor2);
        study2.getAssigendDoctors().add(doctor3);

        patientRepository.save(patient1);
        patientRepository.save(patient2);
        patientRepository.save(patient3);
        patientRepository.save(patient4);

        doctorRepository.save(doctor1);
        doctorRepository.save(doctor2);
        doctorRepository.save(doctor3);

        studyRepository.save(study1);
        studyRepository.save(study2);

        // Add some patients to studies and some measurements
        createPDSM(patient1, doctor1, study1, 100,  200);
        createPDSM(patient1, doctor2, study2, 300,  400);
        createPDSM(patient2, doctor1, study1, 500,  600);
        createPDSM(patient3, doctor2, study2, 700,  800);
        createPDSM(patient4, doctor3, study2, 900, 1000);

        return null;
    }

    private void createPDSM(PatientEntity patient, DoctorEntity doctor, StudyEntity study, int steps1, int steps2) {
        PatientDoctorStudyEntity relationEntity = new PatientDoctorStudyEntity(patient, doctor, study);

        pdsRepository.save(relationEntity);

        createAndStoreMeasurement(relationEntity, steps1);
        createAndStoreMeasurement(relationEntity, steps2);
    }

    private void createAndStoreMeasurement(PatientDoctorStudyEntity relationEntity, int steps1) {
        MeasurementEntity m1 = createTestDbMeasurementEntity(relationEntity, steps1);
        measurementRepository.save(m1);
        relationEntity.getMeasurements().add(m1);
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

    private MeasurementEntity createTestDbMeasurementEntity(PatientDoctorStudyEntity pds, int steps) {
        return new MeasurementEntity(pds, "descr", new Date(), steps);
    }


}
