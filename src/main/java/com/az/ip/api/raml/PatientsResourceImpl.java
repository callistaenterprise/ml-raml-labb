package com.az.ip.api.raml;

import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Measurement;
import com.az.ip.api.gen.model.Patient;
import com.az.ip.api.gen.resource.PatientsResource;
import com.az.ip.api.persistence.jpa.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Created by magnus on 11/07/15.
 */
@Path("patients")
public class PatientsResourceImpl implements PatientsResource {

    private static final Logger LOG = LoggerFactory.getLogger(PatientsResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"username", "firstName", "lastName"});
    private static final String DEFAULT_ORDER_FIELD = "username";

    @Inject
    private PatientRepository repository;

    @Inject
    private StudyRepository studyRepository;

    @Inject
    private MeasurementRepository measurementRepository;

    @Inject
    private PatientDoctorStudyRepository patientDoctorStudyRepository;

    /**
     * Search patients
     *
     * TODO: We need to retain the JAX-RS annotiation from the Patients-interface, don't know why...
     *
     * @param username
     *     Search by username e.g. U11
     * @param orderBy
     *     Order by field: username, firstName, lastName
     * @param order
     * @param page
     *     Skip over a number of pages by specifying the first page for the query, specify 0 for the start page e.g. 0
     * @param size
     *     Specify the size of each page, i.e. the number of elements per page, specify -1 to get all elements (i.e. disable paging) e.g. 20
     * @return
     */
    @Override
    @GET
    @Produces("application/json")
    public GetPatientsResponse getPatients(
        @QueryParam("username")                   String username,
        @QueryParam("orderBy")                    String orderBy,
        @QueryParam("order") @DefaultValue("asc") Order order,
        @QueryParam("page")  @DefaultValue("0")   long page,
        @QueryParam("size")  @DefaultValue("10")  long size) {

        // Find by name?
        if (username != null) {
            LOG.debug("findByName, name: {}", username);
            PatientEntity entity = repository.findByUsername(username);
            return GetPatientsResponse.withJsonOK((entity == null) ? new ArrayList<>() : singletonList(toApiEntity(entity)));
        }

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
            PatientEntity newPatient = repository.save(toNewDbEntity(entity));
            return PostPatientsResponse.withJsonOK(toApiEntity(newPatient));

        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("postPatient request failed, exception: [{}], cause: []{}", ex, ex.getCause());
            return PostPatientsResponse.withJsonConflict(new Error().withCode(-1).withMessage(ex.getMessage()));
        }
    }

    @Override
    public GetPatientsByPatientIdResponse getPatientsByPatientId(String patientId) throws Exception {
        LOG.debug("Get by id: {}", patientId);
        PatientEntity entity = repository.findOne(patientId);

        if (entity == null) {
            LOG.debug("Entity with id: {} was not found", patientId);
            return GetPatientsByPatientIdResponse.withNotFound();

        } else {
            LOG.debug("Found entity with id: {} and username: {}", entity.getId(), entity.getUsername());
            return GetPatientsByPatientIdResponse.withJsonOK(toApiEntity(entity));
        }
    }

    @Override
    public PutPatientsByPatientIdResponse putPatientsByPatientId(String patientId, String accessToken, Patient entity) throws Exception {

        // TODO #1: What to do if not found??? Upsert or error???

        // TODO #2: Do we need to move the id over from the uri-parameter?
        entity.setId(patientId);

        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getUsername());
        repository.save(toExistingDbEntity(entity));

        return PutPatientsByPatientIdResponse.withOK();
    }

    @Override
    public DeletePatientsByPatientIdResponse deletePatientsByPatientId(String patientId, String accessToken) throws Exception {

        // If not found just return ok to behave idempotent...
        repository.delete(patientId);

        return DeletePatientsByPatientIdResponse.withOK();
    }

    /**
     * Return the id's of the studies that the patient is part of
     *
     * @param patientId
     * @return
     * @throws Exception
     */
    @Override
    public GetPatientsByPatientIdStudiesResponse getPatientsByPatientIdStudies(String patientId) throws Exception {

        List<Id> studyIds = patientDoctorStudyRepository.findByPatientId(patientId).stream()
            .map(s -> new Id().withId(s.getStudy().getId()))
            .collect(Collectors.toList());
        return GetPatientsByPatientIdStudiesResponse.withJsonOK(studyIds);
    }

    /**
     * Store a new measurement for the patient
     *
     * @param studyId
     *
     * @param patientId
     *
     * @param measurement
     * @return
     * @throws Exception
     */
    @Override
    public PostPatientsByPatientIdStudiesByStudyIdMeasurementsResponse postPatientsByPatientIdStudiesByStudyIdMeasurements(String studyId, String patientId, Measurement measurement) throws Exception {

        measurementRepository.save(toNewDbMeasurementEntity(findByPatientIdAndStudyId(patientId, studyId), measurement));

        // TODO: Shouldn't we return the new measurement entity here???
        return PostPatientsByPatientIdStudiesByStudyIdMeasurementsResponse.withOK();
    }

    /**
     * Return all measurements for a patient in a specified study
     *
     * @param studyId
     * @param patientId
     *
     * @return
     * @throws Exception
     */
    @Override
    public GetPatientsByPatientIdStudiesByStudyIdMeasurementsResponse getPatientsByPatientIdStudiesByStudyIdMeasurements(String studyId, String patientId) throws Exception {

        List<Measurement> measurements = findByPatientIdAndStudyId(patientId, studyId).getMeasurements().stream()
            .map(m -> toApiMeasurementEntity(m))
            .collect(Collectors.toList());
        return GetPatientsByPatientIdStudiesByStudyIdMeasurementsResponse.withJsonOK(measurements);
    }

    /**
     * Deletes a measurement
     *
     * @param measurementId
     * @param studyId
     *
     * @param patientId
     *
     * @return
     * @throws Exception
     */
    @Override
    public DeletePatientsByPatientIdStudiesByStudyIdMeasurementsByMeasurementIdResponse deletePatientsByPatientIdStudiesByStudyIdMeasurementsByMeasurementId(String measurementId, String studyId, String patientId) throws Exception {


//        PatientEntity     patient     = repository.findOne(patientId);
//        MeasurementEntity measurement = findByStudieId(patient, studyId).getMeasurements().stream()
//            .filter(m -> m.getId().equals(measurementId))
//            .findFirst().get();
//
//        measurementRepository.delete(measurement);

        // Simplified version that relies on that the measurementId is unique...
        measurementRepository.delete(measurementId);

        return DeletePatientsByPatientIdStudiesByStudyIdMeasurementsByMeasurementIdResponse.withOK();
    }

    private PatientDoctorStudyEntity findByPatientIdAndStudyId(String patientId, String studyId) {
        // FIXME. What to do if there are >1 study assigned to one and the same patient? (indicates inconsistent data...)
        List<PatientDoctorStudyEntity> list = patientDoctorStudyRepository.findByPatientIdAndStudyId(patientId, studyId);
        return (list.size() == 0) ? null : list.get(0);
    }


    private List<Patient> findAll(String orderBy, Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

        // TODO. Would be nice to be able to use Java 8 streams here!
        // See https://spring.io/blog/2015/03/26/what-s-new-in-spring-data-fowler
        List<Patient> elements = new ArrayList<>();
        ((pageable == null) ? repository.findAll(sort) : repository.findAll(pageable)).forEach(e -> elements.add(toApiEntity(e)));

        return elements;
    }

    private Patient toApiEntity(PatientEntity p) {
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

    private PatientEntity toNewDbEntity(Patient p) {
        return new PatientEntity(
            p.getUsername(), p.getPatientID(), p.getFirstname(), p.getLastname(), p.getWeight(), p.getHeight()
        );
    }

    private PatientEntity toExistingDbEntity(Patient p) {
        return new PatientEntity(
            p.getId(), p.getVersion(), p.getUsername(), p.getPatientID(), p.getFirstname(), p.getLastname(), p.getWeight(), p.getHeight()
        );
    }

    private Measurement toApiMeasurementEntity(MeasurementEntity m) {
        return new Measurement()
            .withId         (m.getId())
            .withVersion    (m.getVersion())
            .withDescription(m.getDescription())
            .withTimestamp  (m.getTimestamp())
            .withSteps      (m.getSteps());
    }

    private MeasurementEntity toNewDbMeasurementEntity(PatientDoctorStudyEntity p, Measurement m) {
        return new MeasurementEntity(
            p, m.getDescription(), m.getTimestamp(), m.getSteps()
        );
    }


}
