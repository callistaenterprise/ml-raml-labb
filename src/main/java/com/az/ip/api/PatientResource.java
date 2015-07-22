package com.az.ip.api;

import com.az.ip.api.model.*;
import com.az.ip.api.model.Error;
import com.az.ip.api.persistence.jpa.PatientRepository;
import com.az.ip.api.resource.Patients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by magnus on 11/07/15.
 */
@Path("patients")
public class PatientResource implements Patients {

    private static final Logger LOG = LoggerFactory.getLogger(PatientResource.class);

    @Inject
    PatientRepository repository;

    @Override
    public Patients.GetPatientsByUsernameResponse getPatientsByUsername(String username) throws Exception {
        return GetPatientsByUsernameResponse.withJsonOK(createPatient(repository.findByUsername(username)));
    }

    @Override
    @GET
    @Produces("application/json")
    public Patients.GetPatientsResponse getPatients(
            @QueryParam("query")                       String query,
            @QueryParam("orderBy")                     String orderBy,
            @QueryParam("order") @DefaultValue("desc") Patients.Order order,
            @QueryParam("page")  @DefaultValue("0")    long page,
            @QueryParam("size")  @DefaultValue("10")   long size)
            throws Exception {

        LOG.debug("getPatients, page: {}, size: {}", page, size);
        List<Patient> patients = new ArrayList<>();
        Pageable      pageable     = (size == -1) ? null : new PageRequest((int)page, (int)size);

        repository.findAll(pageable).forEach(p -> patients.add(createPatient(p)));

        return GetPatientsResponse.withJsonOK(patients);
    }

    @Override
    public Patients.PostPatientsResponse postPatients(String accessToken, Patient entity) throws Exception {
        try {
            repository.save(createPatient(entity));
            return Patients.PostPatientsResponse.withOK();
        } catch (RuntimeException ex) {
            System.err.println("EX: " + ex);
            System.err.println("EX.cause: " + ex.getCause());
            return Patients.PostPatientsResponse.withJsonConflict(new Error().withCode(-1).withMessage(ex.getMessage()));
        }
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