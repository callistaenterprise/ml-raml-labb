package com.az.ip.api.services.impl;

import com.az.ip.api.gen.model.*;
import com.az.ip.api.persistence.jpa.*;
import com.az.ip.api.services.api.StudiesResource;
import com.az.ip.api.services.model.Pageable;
import com.az.ip.api.services.model.Sortable;
import com.az.ip.api.services.util.HttpConflictException;
import com.az.ip.api.services.util.HttpNotFoundException;
import com.az.ip.api.services.util.HttpUnprocessableEntityException;
import com.az.ip.api.services.util.SpringMvcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Created by magnus on 21/08/15.
 */
@RestController
public class StudiesResourceImpl implements StudiesResource {

    private static final Logger LOG = LoggerFactory.getLogger(StudiesResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"name"});
    private static final String DEFAULT_ORDER_FIELD = "name";

    @Inject
    SpringMvcUtil util;

    @Inject
    private StudyRepository repository;

    @Inject
    private DoctorRepository doctorRepository;

    @Inject
    private PatientDoctorStudyRepository patientDoctorStudyRepository;

    @Override
    public ResponseEntity<List<Study>> findStudies(
        @RequestParam(required=false) String name,
        @ModelAttribute Sortable sort,
        @ModelAttribute Pageable page) {

        // Find by name?
        if (name != null) {
            LOG.debug("findByName, name: {}", name);
            StudyEntity entity = repository.findByName(name);
            List<Study> response = (entity == null) ? new ArrayList<>() : singletonList(toApiEntity(entity));

            return util.createOkResponse(response);
        }

        // Ordinary find...
        LOG.debug("find, orderBy: {}, order: {}, page: {}, size: {}", sort.getOrderBy(), sort.getOrder(), page.getPage(), page.getSize());

        // TODO: Can we make this a @DefaultValue instead?
        if (sort.getOrderBy() == null) sort.setOrderBy(DEFAULT_ORDER_FIELD);

        // TODO: Extract Common code!
        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(sort.getOrderBy())) {
            String errMsg = "Order field [" + sort.getOrderBy() + "] must be on of: " + ORDER_FIELDS;
            LOG.error("find request failed: " + errMsg);

            throw new HttpUnprocessableEntityException(errMsg);
        }

        return util.createOkResponse(findAll(sort.getOrderBy(), sort.getOrder(), page.getPage(), page.getSize()));
    }

    @Override
    public ResponseEntity<Study> createStudy(@RequestBody Study entity) {
        try {
            StudyEntity newEntity = repository.save(toNewDbEntity(entity));
            LOG.debug("Created entity with id: {}", newEntity.getId());
            return util.createOkResponse(toApiEntity(newEntity));

        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("post request failed, exception: [{}], cause: []{}", ex,toString(), ex.getCause());
            throw new HttpConflictException(ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<Study> getStudy(@PathVariable String studyId) {
        LOG.debug("Get by id: {}", studyId);
        StudyEntity entity = repository.findOne(studyId);

        if (entity == null) {
            String errMsg = "Entity with id: " + studyId + " was not found";
            LOG.debug(errMsg);
            throw new HttpNotFoundException(errMsg);

        } else {
            LOG.debug("Found entity with id: {} and name: {}", entity.getId(), entity.getName());
            return util.createOkResponse(toApiEntity(entity));
        }
    }

    @Override
    public void updateStudy(@PathVariable String studyId, @RequestBody Study entity) {
        // TODO #1: What to do if not found??? Upsert or error???

        // TODO #2: Do we need to move the id over from the uri-parameter?
        entity.setId(studyId);

        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getName());
        try {
            repository.save(toExistingDbEntity(entity));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new HttpConflictException(ex.getMessage());
        }
    }

    @Override
    public void deleteStudy(@PathVariable String studyId) {
        // If not found just return ok to behave idempotent...
        LOG.debug("Delete by id: {}", studyId);
        repository.delete(studyId);
    }

    @Override
    public ResponseEntity<List<Id>> getAssignedDoctors(@PathVariable String studyId) {
        LOG.debug("getAssignedDoctors to study with id: {}", studyId);
        StudyEntity entity = repository.findOne(studyId);
        List<Id> doctorIds = entity.getAssigendDoctors().stream().map(d -> new Id().withId(d.getId())).collect(Collectors.toList());

        LOG.debug("getAssignedDoctors found: #{}", doctorIds.size());

        return util.createOkResponse(doctorIds);
    }

    @Override
    public void assignDoctorToStudy(@PathVariable String studyId, @RequestBody Id doctorIdRef) {
        String doctorId = doctorIdRef.getId();

        LOG.debug("Assign doctor with id {} to study with id: {}", doctorId, studyId);

        StudyEntity  study  = repository.findOne(studyId);
        DoctorEntity doctor = doctorRepository.findOne(doctorId);
        study.getAssigendDoctors().add(doctor);

        repository.save(study);
    }

    @Override
    public void removeDoctorFromStudy(@PathVariable String studyId, @PathVariable String doctorId) {
        LOG.debug("Remove doctor with id {} from study with id: {}", doctorId, studyId);

        StudyEntity  study  = repository.findOne(studyId);
        DoctorEntity doctor = doctorRepository.findOne(doctorId);
        study.getAssigendDoctors().remove(doctor);

        repository.save(study);
    }

    @Override
    public ResponseEntity<List<Measurement>> getMeasurementsInStudy(@PathVariable String studyId) {

        LOG.debug("Find all measurements for study with id: {}", studyId);

        StudyEntity study = repository.findOne(studyId);
        List<Measurement> list = patientDoctorStudyRepository.findByStudy(study).stream()
            .flatMap(m -> m.getMeasurements().stream())
            .map(m -> toApiMeasurementEntity(m))
            .collect(Collectors.toList());

        LOG.debug("getMeasurementsInStudy found: #{}", list.size());

        return util.createOkResponse(list);
    }


    // TODO: Extract Common code!
    private List<Study> findAll(String orderBy, Sortable.Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Sortable.Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        org.springframework.data.domain.Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

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
            .withSteps(m.getSteps());
    }


}
