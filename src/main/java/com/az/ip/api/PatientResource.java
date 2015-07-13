package com.az.ip.api;

import com.az.ip.api.model.Patient;
import com.az.ip.api.resource.Patients;

import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by magnus on 11/07/15.
 */
@Path("patients")
public class PatientResource implements Patients {

    @Override
    public Patients.GetPatientsByUsernameResponse getPatientsByUsername(String username) throws Exception {

        return GetPatientsByUsernameResponse.withJsonOK(createTestPatient(username));
    }

    @Override
    public GetPatientsResponse getPatients() throws Exception {

        List<Patient> patients = new ArrayList<>();
        patients.add(createTestPatient("U1"));
        patients.add(createTestPatient("U2"));
        patients.add(createTestPatient("U3"));

        return Patients.GetPatientsResponse.withJsonOK(patients);
    }

    @Override
    public Patients.PostPatientsResponse postPatients(String accessToken, Patient entity) throws Exception {
        return Patients.PostPatientsResponse.withOK();
    }

    private Patient createTestPatient(String username) {
        return new Patient().withUsername(username).withPatientID("1234").withFirstname("F1").withLastname("L1").withWeight(100).withHeight(200);
    }
}