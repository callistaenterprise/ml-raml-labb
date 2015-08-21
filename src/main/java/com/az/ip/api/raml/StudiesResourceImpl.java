package com.az.ip.api.raml;

import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Measurement;
import com.az.ip.api.gen.model.Study;
import com.az.ip.api.gen.resource.StudiesResource;
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
@Path("studies")
public class StudiesResourceImpl implements StudiesResource {

    private static final Logger LOG = LoggerFactory.getLogger(StudiesResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"name"});
    private static final String DEFAULT_ORDER_FIELD = "name";

    @Inject
    private StudyRepository repository;

    @Inject
    private DoctorRepository doctorRepository;

    @Inject
    private PatientDoctorStudyRepository patientDoctorStudyRepository;

    @Override
    @GET
    @Produces({
            "application/json"
    })
    public GetStudiesResponse getStudies(
            @QueryParam("name")                       String name,
            @QueryParam("orderBy")                    String orderBy,
            @QueryParam("order") @DefaultValue("asc") Order order,
            @QueryParam("page")  @DefaultValue("0")   long page,
            @QueryParam("size")  @DefaultValue("20")  long size) {

        // Find by name?
        if (name != null) {
            LOG.debug("findByName, name: {}", name);
            StudyEntity entity = repository.findByName(name);
            return GetStudiesResponse.withJsonOK((entity == null) ? new ArrayList<>() : singletonList(toApiEntity(entity)));
        }

        // Ordinary find...
        LOG.debug("find, orderBy: {}, order: {}, page: {}, size: {}", orderBy, order, page, size);

        // TODO: Can we make this a @DefaultValue instead?
        if (orderBy == null) orderBy = DEFAULT_ORDER_FIELD;

        // TODO: Extract Common code!
        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(orderBy)) {
            String errMsg = "Order field [" + orderBy + "] must be on of: " + ORDER_FIELDS;
            LOG.error("find request failed: " + errMsg);
            return GetStudiesResponse.withJsonUnprocessableEntity(new Error().withCode(-1).withMessage(errMsg));
        }

        return GetStudiesResponse.withJsonOK(findAll(orderBy, order, page, size));
    }

    @Override
    public PostStudiesResponse postStudies(String accessToken, Study entity) throws Exception {
        try {
            StudyEntity newStudy = repository.save(toNewDbEntity(entity));
            return PostStudiesResponse.withJsonOK(toApiEntity(newStudy));
        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("postPatient request failed, exception: [{}], cause: []{}", ex, ex.getCause());
            return PostStudiesResponse.withJsonConflict(new Error().withCode(-1).withMessage(ex.getMessage()));
        }
    }

    @Override
    public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) throws Exception {
        LOG.debug("Get by id: {}", studyId);
        StudyEntity entity = repository.findOne(studyId);

        if (entity == null) {
            LOG.debug("Entity with id: {} was not found", studyId);
            return GetStudiesByStudyIdResponse.withNotFound();

        } else {
            LOG.debug("Found entity with id: {} and name: {}", entity.getId(), entity.getName());
            return GetStudiesByStudyIdResponse.withJsonOK(toApiEntity(entity));
        }
    }

    @Override
    public PutStudiesByStudyIdResponse putStudiesByStudyId(String studyId, String accessToken, Study entity) throws Exception {

        // TODO #1: What to do if not found??? Upsert or error???

        // TODO #2: Do we need to move the id over from the uri-parameter?
        entity.setId(studyId);

        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getName());
        repository.save(toExistingDbEntity(entity));

        return PutStudiesByStudyIdResponse.withOK();
    }

    @Override
    public DeleteStudiesByStudyIdResponse deleteStudiesByStudyId(String studyId, String accessToken) throws Exception {

        // If not found just return ok to behave idempotent...
        repository.delete(studyId);

        return DeleteStudiesByStudyIdResponse.withOK();
    }

    @Override
    public GetStudiesByStudyIdMeasurementsResponse getStudiesByStudyIdMeasurements(String studyId) throws Exception {

        StudyEntity study = repository.findOne(studyId);
        List<Measurement> list = patientDoctorStudyRepository.findByStudy(study).stream()
            .flatMap(m -> m.getMeasurements().stream())
            .map(m -> toApiMeasurementEntity(m))
            .collect(Collectors.toList());
        return GetStudiesByStudyIdMeasurementsResponse.withJsonOK(list);
    }

    /**
     * Add a doctor to this study
     *
     * @param studyId
     *
     * @param entity
     * @return
     * @throws Exception
     */
    @Override
    public PostStudiesByStudyIdAssignedDoctorsResponse postStudiesByStudyIdAssignedDoctors(String studyId, Id entity) throws Exception {
        String doctorId = entity.getId();

        StudyEntity study = repository.findOne(studyId);
        DoctorEntity doctor = doctorRepository.findOne(doctorId);
        study.getAssigendDoctors().add(doctor);

        repository.save(study);

        return PostStudiesByStudyIdAssignedDoctorsResponse.withOK();
    }

    /**
     * Get doctors assigned to this study
     *
     * @param studyId
     * @return
     * @throws Exception
     */
    @Override
    public GetStudiesByStudyIdAssignedDoctorsResponse getStudiesByStudyIdAssignedDoctors(String studyId) throws Exception {
        StudyEntity entity = repository.findOne(studyId);
        List<Id> doctorIds = entity.getAssigendDoctors().stream().map(d -> new Id().withId(d.getId())).collect(Collectors.toList());
        return GetStudiesByStudyIdAssignedDoctorsResponse.withJsonOK(doctorIds);
    }

    @Override
    public DeleteStudiesByStudyIdAssignedDoctorsByDoctorIdResponse deleteStudiesByStudyIdAssignedDoctorsByDoctorId(String doctorId, String studyId) throws Exception {

        StudyEntity study = repository.findOne(studyId);
        DoctorEntity doctor = doctorRepository.findOne(doctorId);
        study.getAssigendDoctors().remove(doctor);

        repository.save(study);

        return DeleteStudiesByStudyIdAssignedDoctorsByDoctorIdResponse.withOK();
    }

    // TODO: Extract Common code!
    private List<Study> findAll(String orderBy, Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

        // TODO. Would be nice to be able to use Java 8 streams here!
        // See https://spring.io/blog/2015/03/26/what-s-new-in-spring-data-fowler
        List<Study> elements = new ArrayList<>();
        ((pageable == null) ? repository.findAll(sort) : repository.findAll(pageable)).forEach(e -> elements.add(toApiEntity(e)));

        return elements;
    }

    private Study toApiEntity(StudyEntity entity) {
        return new Study()
            .withId(entity.getId())
            .withVersion(entity.getVersion())
            .withName(entity.getName())
            .withDescription(entity.getDescription())
            .withStartdate(entity.getStartdate())
            .withEnddate(entity.getEnddate());
    }

    private StudyEntity toNewDbEntity(Study entity) {
        return new StudyEntity(
            entity.getName(),
            entity.getDescription(),
            entity.getStartdate(),
            entity.getEnddate()
        );
    }

    private StudyEntity toExistingDbEntity(Study entity) {
        return new StudyEntity(
            entity.getId(),
            entity.getVersion(),
            entity.getName(),
            entity.getDescription(),
            entity.getStartdate(),
            entity.getEnddate()
        );
    }

    // TODO: Move to a common library with static methods that resourceImpl classesa (and other) can import?
    private Measurement toApiMeasurementEntity(MeasurementEntity m) {
        return new Measurement()
            .withId         (m.getId())
            .withVersion    (m.getVersion())
            .withDescription(m.getDescription())
            .withTimestamp  (m.getTimestamp())
            .withSteps      (m.getSteps());
    }

}
