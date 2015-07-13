package com.az.ip.api;

import com.az.ip.api.model.Patient;
import com.az.ip.api.persistence.jpa.PatientRepository;
import com.az.ip.api.resource.Patients;

import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by magnus on 11/07/15.
 */
@Path("patients")
public class PatientResource implements Patients {

    @Inject
    PatientRepository repository;

    @Override
    public Patients.GetPatientsByUsernameResponse getPatientsByUsername(String username) throws Exception {

        return GetPatientsByUsernameResponse.withJsonOK(createPatient(repository.findByUsername(username)));
//        return GetPatientsByUsernameResponse.withJsonOK(createTestPatient(username));
    }

    @Override
    public GetPatientsResponse getPatients() throws Exception {
        List<Patient> patients = new ArrayList<>();
        repository.findAll().forEach(p -> patients.add(createPatient(p)));
        return GetPatientsResponse.withJsonOK(patients);

//        patients.add(createTestPatient("U1"));
//        patients.add(createTestPatient("U2"));
//        patients.add(createTestPatient("U3"));
//
//        return Patients.GetPatientsResponse.withJsonOK(patients);
    }

    @Override
    public Patients.PostPatientsResponse postPatients(String accessToken, Patient entity) throws Exception {
        repository.save(createPatient(entity));
        return Patients.PostPatientsResponse.withOK();
    }

    private Patient createPatient(com.az.ip.api.persistence.jpa.Patient p) {
        return new Patient().withUsername(p.getUsername()).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }

    private com.az.ip.api.persistence.jpa.Patient createPatient(Patient p) {
        return new com.az.ip.api.persistence.jpa.Patient(
            p.getUsername(), p.getPatientID(), p.getFirstname(), p.getLastname(), p.getWeight(), p.getHeight()
        );
    }

    private Patient createTestPatient(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }
}