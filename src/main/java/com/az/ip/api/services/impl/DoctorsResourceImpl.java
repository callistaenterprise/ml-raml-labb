package com.az.ip.api.services.impl;

import com.az.ip.api.gen.model.*;
import com.az.ip.api.persistence.jpa.*;
import com.az.ip.api.services.api.DoctorsResource;
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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Created by magnus on 24/08/15.
 */
@RestController
public class DoctorsResourceImpl implements DoctorsResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorsResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"username", "firstName", "lastName"});
    private static final String DEFAULT_ORDER_FIELD = "username";

    @Inject
    SpringMvcUtil util;

    @Inject
    private DoctorRepository repository;

    @Inject
    private PatientDoctorStudyRepository patientDoctorStudyRepository;


    @Override
    public ResponseEntity<List<Doctor>> findDoctors(
        @RequestParam(required=false) String username,
        @ModelAttribute Sortable sort,
        @ModelAttribute Pageable page) {

        // Find by name?
        if (username != null) {
            LOG.debug("findByName, username: {}", username);
            DoctorEntity entity = repository.findByUsername(username);

            List<Doctor> response = (entity == null) ? new ArrayList<>() : singletonList(toApiEntity(entity));

            return util.createOkResponse(response);

        }

        // Ordinary find...
        LOG.debug("find, orderBy: {}, order: {}, page: {}, size: {}", sort.getOrderBy(), sort.getOrder(), page.getPage(), page.getSize());

        // TODO: Can we make this a @DefaultValue instead?
        if (sort.getOrderBy() == null) sort.setOrderBy(DEFAULT_ORDER_FIELD);

        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(sort.getOrderBy())) {
            String errMsg = "Order field [" + sort.getOrderBy() + "] must be on of: " + ORDER_FIELDS;
            LOG.error("getDoctors request failed: " + errMsg);

            throw new HttpUnprocessableEntityException(errMsg);
        }

        return util.createOkResponse(findAll(sort.getOrderBy(), sort.getOrder(), page.getPage(), page.getSize()));
    }

    @Override
    public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor entity) {
        try {
            DoctorEntity newEntity = repository.save(toNewDbEntity(entity));
            LOG.debug("Created entity with id: {}", newEntity.getId());
            return util.createOkResponse(toApiEntity(newEntity));

        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("post request failed, exception: [{}], cause: []{}", ex,toString(), ex.getCause());
            throw new HttpConflictException(ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<Doctor> getDoctor(@PathVariable String doctorId) {
        LOG.debug("Get by id: {}", doctorId);
        DoctorEntity entity = repository.findOne(doctorId);

        if (entity == null) {
            String errMsg = "Entity with id: " + doctorId + " was not found";
            LOG.debug(errMsg);
            throw new HttpNotFoundException(errMsg);

        } else {
            LOG.debug("Found entity with id: {} and name: {}", entity.getId(), entity.getUsername());
            return util.createOkResponse(toApiEntity(entity));
        }
    }

    @Override
    public void updateDoctor(@PathVariable String doctorId, @RequestBody Doctor entity) {
        // TODO #1: What to do if not found??? Upsert or error???

        // TODO #2: Do we need to move the id over from the uri-parameter?
        entity.setId(doctorId);

        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getUsername());
        try {
            repository.save(toExistingDbEntity(entity));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new HttpConflictException(ex.getMessage());
        }
    }

    @Override
    public void deleteDoctor(@PathVariable String doctorId) {
        // If not found just return ok to behave idempotent...
        LOG.debug("Delete by id: {}", doctorId);
        repository.delete(doctorId);
    }

    @Override
    public ResponseEntity<List<Id>> getAssignedInStudies(@PathVariable String doctorId) {
        LOG.debug("getAssignedInStudies, doctorIdd: {}", doctorId);
        DoctorEntity entity = repository.findOne(doctorId);
        List<Id> studyIds = entity.getAssigendInStudies().stream().map(s -> new Id().withId(s.getId())).collect(Collectors.toList());

        LOG.debug("getAssignedInStudies found: #{}", studyIds.size());

        return util.createOkResponse(studyIds);
    }

    @Override
    public void assignPatientByDoctorToStudy(@PathVariable String doctorId, @PathVariable String studyId, @RequestBody Id patientIdRef) {
        String patientId = patientIdRef.getId();

        // FIXME. No optimal way to create the mapping entity...
        patientDoctorStudyRepository.save(
            new PatientDoctorStudyEntity(
                new PatientEntity(patientId, 0, ".", null, ".", ".", null, null),
                new DoctorEntity(doctorId, 0, ".", ".", "."),
                new StudyEntity(studyId, 0, ".", null, null, null)
            )
        );
    }

    @Override
    public ResponseEntity<List<Id>> getPatientsAssignedByDoctorInStudy(@PathVariable String doctorId, @PathVariable String studyId) {

        // FIXME. No optimal way to find mapping entities...
        List<PatientDoctorStudyEntity> relationList = patientDoctorStudyRepository.findByStudyAndDoctor(
            new StudyEntity(studyId, 0, ".", null, null, null),
            new DoctorEntity(doctorId, 0, ".", ".", ".")
        );

        List<Id> patientIds = relationList.stream().map(r -> new Id().withId(r.getPatient().getId())).collect(Collectors.toList());

        return util.createOkResponse(patientIds);
    }

    @Override
    public void removePatientAssignedByDoctorInStudy(@PathVariable String doctorId, @PathVariable String studyId, @PathVariable String patientId) {
        // FIXME. No optimal way to delete a patient from a study...

        List<PatientDoctorStudyEntity> relationList = patientDoctorStudyRepository.findByPatientAndDoctorAndStudy(
            new PatientEntity(patientId, 0, ".", null, ".", ".", null, null),
            new DoctorEntity(doctorId, 0, ".", ".", "."),
            new StudyEntity(studyId, 0, ".", null, null, null)
        );

        if (relationList.size() > 1) throw new RuntimeException("Inconsistenct detected, expecte 0 or 1 bug found: " + relationList.size());

        if (relationList.size() == 1) {
            patientDoctorStudyRepository.delete(relationList.get(0));
        }
    }

    // TODO: Extract Common code!
    private List<Doctor> findAll(String orderBy, Sortable.Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Sortable.Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        org.springframework.data.domain.Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

        // TODO. Would be nice to be able to use Java 8 streams here!
        // See https://spring.io/blog/2015/03/26/what-s-new-in-spring-data-fowler
        List<Doctor> elements = new ArrayList<>();
        ((pageable == null) ? repository.findAll(sort) : repository.findAll(pageable)).forEach(e -> elements.add(toApiEntity(e)));

        return elements;
    }

    private Doctor toApiEntity(DoctorEntity p) {
        return new Doctor()
            .withId       (p.getId())
            .withVersion  (p.getVersion())
            .withUsername (p.getUsername())
            .withFirstname(p.getFirstname())
            .withLastname (p.getLastname());
    }

    private DoctorEntity toNewDbEntity(Doctor p) {
        return new DoctorEntity(
            p.getUsername(), p.getFirstname(), p.getLastname()
        );
    }

    private DoctorEntity toExistingDbEntity(Doctor p) {
        return new DoctorEntity(
            p.getId(), p.getVersion(), p.getUsername(), p.getFirstname(), p.getLastname()
        );
    }

}
