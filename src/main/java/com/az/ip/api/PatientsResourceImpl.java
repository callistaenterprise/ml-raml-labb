package com.az.ip.api;

import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Patient;
import com.az.ip.api.gen.resource.PatientsResource;
import com.az.ip.api.persistence.jpa.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by magnus on 11/07/15.
 */
@Path("patients")
public class PatientsResourceImpl implements PatientsResource {

    private static final Logger LOG = LoggerFactory.getLogger(PatientsResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"username", "firstName", "lastName"});
    private static final String DEFAULT_ORDER_FIELD = "username";

    @Inject
    PatientRepository repository;

    /**
     * Search patients
     *
     * TODO: We need to retain the JAX-RS annotiation from the Patients-interface, don't know why...
     *
     * @param query
     *     JSON array [{"field1","value1","operator1"},{"field2","value2","operator2"},...,{"fieldN","valueN","operatorN"}] with valid searchable fields: lastName
     *      e.g. ["lastName", "l", "matches"]
     *
     * @param orderBy
     *     Order by field: username, firstName, lastName
     *
     * @param order
     * @param page
     *     Skip over a number of pages by specifying the first page for the query, specify 0 for the start page e.g. 2
     * @param size
     *     Specify the size of each page, i.e. the number of elements per page, specify -1 to get all elements (i.e. disable paging) e.g. 80
     * @return
     */
    @Override
    @GET
    @Produces("application/json")
    public GetPatientsResponse getPatients(
        @QueryParam("query")                      String query,
        @QueryParam("orderBy")                    String orderBy,
        @QueryParam("order") @DefaultValue("asc") Order order,
        @QueryParam("page")  @DefaultValue("0")   long page,
        @QueryParam("size")  @DefaultValue("10")  long size) {

        LOG.debug("find, orderBy: {}, order: {}, page: {}, size: {}", orderBy, order, page, size);

        // TODO: Can we make this a @DefaultValue instead?
        if (orderBy == null) orderBy = DEFAULT_ORDER_FIELD;

        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(orderBy)) {
            String errMsg = "Order field [" + orderBy + "] must be on of: " + ORDER_FIELDS;
            LOG.error("getPatients request failed: " + errMsg);
            return GetPatientsResponse.withJsonUnprocessableEntity(new Error().withCode(-1).withMessage(errMsg));
        }

        return GetPatientsResponse.withJsonOK(findAll(orderBy, order, page, size));
    }

    /**
     * Create a new patient
     *
     * @param accessToken
     *     The access token provided by the authentication application e.g. AABBCCDD
     * @param entity
     *      e.g. {
     *       "username":"U1",
     *       "patientID":"1234",
     *       "firstname":"F1",
     *       "lastname":"L1",
     *       "weight":100,
     *       "height":200
     * @return
     * @throws Exception
     */
    @Override
    public PostPatientsResponse postPatients(String accessToken, Patient entity) throws Exception {
        try {
            repository.save(toNewJpaPatient(entity));
            return PostPatientsResponse.withOK();
        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("postPatient request failed, exception: [{}], cause: []{}", ex, ex.getCause());
            return PostPatientsResponse.withJsonConflict(new Error().withCode(-1).withMessage(ex.getMessage()));
        }
    }

    /**
     * Find an existing patient
     *
     * @param username
     * @return
     * @throws Exception
     */
    @Override
    public GetPatientsByUsernameResponse getPatientsByUsername(String username) throws Exception {
        LOG.debug("Get patient with username: {}", username);
        com.az.ip.api.persistence.jpa.Patient p = repository.findByUsername(username);

        if (p == null) {
            LOG.debug("Patient with username: {} was not found", username);
            return GetPatientsByUsernameResponse.withNotFound();

        } else {
            LOG.debug("Found patient with id: {} and first-name: {}", p.getId(), p.getFirstname());
            return GetPatientsByUsernameResponse.withJsonOK(toApiPatient(p));
        }
    }

    /**
     * Update an existing patient
     *
     * @param username
     * @param accessToken
     *     The access token provided by the authentication application e.g. AABBCCDD
     * @param entity
     *      e.g. {
     *       "username":"U1",
     *       "patientID":"1234",
     *       "firstname":"F1",
     *       "lastname":"L1",
     *       "weight":100,
     *       "height":200
     *     }
     *
     * @return
     * @throws Exception
     */
    @Override
    @Transactional
    public PutPatientsByUsernameResponse putPatientsByUsername(String username, String accessToken, Patient entity) throws Exception {

        // Must @Transactional be used???

        // TODO: What to do if not found??? Upsert or error???
        LOG.debug("Update patient: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getFirstname());
        repository.save(toExistingJpaPatient(entity));

        return PutPatientsByUsernameResponse.withOK();
    }

    /**
     * Delete an existing patient
     *
     * @param username
     * @param accessToken
     *     The access token provided by the authentication application e.g. AABBCCDD
     * @return
     * @throws Exception
     */
    @Override
    public DeletePatientsByUsernameResponse deletePatientsByUsername(String username, String accessToken) throws Exception {
        // TODO: Simplify delete operation. E.g. use the a technical PK, id, in the rest api...
        LOG.debug("Delete patient with username: {}", username);
        com.az.ip.api.persistence.jpa.Patient p = repository.findByUsername(username);

        // If not found just return ok to behave idempotent...
        if (p == null) {
            LOG.debug("Patient with username: {} not found, just exit", username);
        } else {
            LOG.debug("Now remove patient with id: {}", p.getId());
            repository.delete(p.getId());
        }

        return DeletePatientsByUsernameResponse.withOK();
    }

    private List<Patient> findAll(String orderBy, Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

        // TODO. Would be nice to be able to use Java 8 streams here!
        // See https://spring.io/blog/2015/03/26/what-s-new-in-spring-data-fowler
        List<Patient> elements = new ArrayList<>();
        ((pageable == null) ? repository.findAll(sort) : repository.findAll(pageable)).forEach(e -> elements.add(toApiPatient(e)));

        return elements;
    }

    private Patient toApiPatient(com.az.ip.api.persistence.jpa.Patient p) {
        return new Patient()
            .withId       (p.getId())
            .withVersion  (p.getVersion())
            .withUsername (p.getUsername())
            .withPatientID(p.getPatientID())
            .withFirstname(p.getFirstname())
            .withLastname (p.getLastname())
            .withWeight   (p.getWeight())
            .withHeight   (p.getHeight());
    }

    private com.az.ip.api.persistence.jpa.Patient toNewJpaPatient(Patient p) {
        return new com.az.ip.api.persistence.jpa.Patient(
            p.getUsername(), p.getPatientID(), p.getFirstname(), p.getLastname(), p.getWeight(), p.getHeight()
        );
    }

    private com.az.ip.api.persistence.jpa.Patient toExistingJpaPatient(Patient p) {
        return new com.az.ip.api.persistence.jpa.Patient(
            p.getId(), p.getVersion(), p.getUsername(), p.getPatientID(), p.getFirstname(), p.getLastname(), p.getWeight(), p.getHeight()
        );
    }
}