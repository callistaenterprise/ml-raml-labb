package com.az.ip.api;

import com.az.ip.api.model.Error;
import com.az.ip.api.model.Patient;
import com.az.ip.api.persistence.jpa.PatientRepository;
import com.az.ip.api.resource.Patients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by magnus on 11/07/15.
 */
@Path("patients")
public class PatientResource implements Patients {

    private static final Logger LOG = LoggerFactory.getLogger(PatientResource.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"username", "firstName", "lastName"});
    private static final String DEFAULT_ORDER_FIELD = "username";

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
        @QueryParam("query")                      String query,
        @QueryParam("orderBy")                    String orderBy,
        @QueryParam("order") @DefaultValue("asc") Patients.Order order,
        @QueryParam("page")  @DefaultValue("0")   long page,
        @QueryParam("size")  @DefaultValue("10")  long size)
        throws Exception {

        LOG.debug("getPatients, orderBy: {}, order: {}, page: {}, size: {}", page, size);

        // TODO: Can we make this a @DefaultValue instead?
        if (orderBy == null) orderBy = DEFAULT_ORDER_FIELD;

        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(orderBy)) {
            String errMsg = "Order field [" + orderBy + "] must be on of: " + ORDER_FIELDS;
            LOG.error("getPatients request failed: " + errMsg);
            return Patients.GetPatientsResponse.withJsonUnprocessableEntity(new Error().withCode(-1).withMessage(errMsg));
        }

        return GetPatientsResponse.withJsonOK(findAll(orderBy, order, page, size));
    }

    private List<Patient> findAll(String orderBy, Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

        // TODO. Would be nice to be able to use Java 8 streams here!
        // See https://spring.io/blog/2015/03/26/what-s-new-in-spring-data-fowler
        List<Patient> elements = new ArrayList<>();
        ((pageable == null) ? repository.findAll(sort) : repository.findAll(pageable)).forEach(e -> elements.add(createPatient(e)));

        return elements;
    }

    @Override
    public Patients.PostPatientsResponse postPatients(String accessToken, Patient entity) throws Exception {
        try {
            repository.save(createPatient(entity));
            return Patients.PostPatientsResponse.withOK();
        } catch (RuntimeException ex) {
            LOG.error("postPatient request failed, exception: [{}], cause: []{}", ex, ex.getCause());
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