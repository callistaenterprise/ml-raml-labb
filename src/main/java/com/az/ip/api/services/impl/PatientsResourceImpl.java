package com.az.ip.api.services.impl;

import com.az.ip.api.gen.model.*;
import com.az.ip.api.persistence.jpa.*;
import com.az.ip.api.services.api.PatientsResource;
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
public class PatientsResourceImpl implements PatientsResource {

    private static final Logger LOG = LoggerFactory.getLogger(PatientsResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"username", "firstName", "lastName"});
    private static final String DEFAULT_ORDER_FIELD = "username";

    @Inject
    SpringMvcUtil util;

    @Inject
    private PatientRepository repository;

    @Inject
    private StudyRepository studyRepository;

    @Inject
    private MeasurementRepository measurementRepository;

    @Inject
    private PatientDoctorStudyRepository patientDoctorStudyRepository;

    @Override
    public ResponseEntity<List<Patient>> findPatients(@RequestParam String username, @ModelAttribute Sortable sort, @ModelAttribute Pageable page) {
        // Find by name?
        if (username != null) {
            LOG.debug("findByName, name: {}", username);
            PatientEntity entity = repository.findByUsername(username);

            List<Patient> response = (entity == null) ? new ArrayList<>() : singletonList(toApiEntity(entity));

            return util.createOkResponse(response);
        }

        LOG.debug("find, orderBy: {}, order: {}, page: {}, size: {}", sort.getOrderBy(), sort.getOrder(), page.getPage(), page.getSize());

        // TODO: Can we make this a @DefaultValue instead?
        if (sort.getOrderBy() == null) sort.setOrderBy(DEFAULT_ORDER_FIELD);

        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(sort.getOrderBy())) {
            String errMsg = "Order field [" + sort.getOrderBy() + "] must be on of: " + ORDER_FIELDS;
            LOG.error("getPatients request failed: " + errMsg);

            throw new HttpUnprocessableEntityException(errMsg);
        }

        return util.createOkResponse(findAll(sort.getOrderBy(), sort.getOrder(), page.getPage(), page.getSize()));
    }

    @Override
    public ResponseEntity<Patient> createPatient(@RequestBody Patient entity) {
        try {
            PatientEntity newEntity = repository.save(toNewDbEntity(entity));
            LOG.debug("Created entity with id: {}", newEntity.getId());
            return util.createOkResponse(toApiEntity(newEntity));

        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("post request failed, exception: [{}], cause: []{}", ex,toString(), ex.getCause());
            throw new HttpConflictException(ex.getMessage());
        }
    }

    @Override
    public ResponseEntity<Patient> getPatient(@PathVariable String patientId) {
        LOG.debug("Get by id: {}", patientId);
        PatientEntity entity = repository.findOne(patientId);

        if (entity == null) {
            String errMsg = "Entity with id: " + patientId + " was not found";
            LOG.debug(errMsg);
            throw new HttpNotFoundException(errMsg);

        } else {
            LOG.debug("Found entity with id: {} and username: {}", entity.getId(), entity.getUsername());
            return util.createOkResponse(toApiEntity(entity));
        }
    }

    @Override
    public void updatePatient(@PathVariable String patientId, @RequestBody Patient entity) {
        // TODO #1: What to do if not found??? Upsert or error???

        // TODO #2: Do we need to move the id over from the uri-parameter?
        entity.setId(patientId);

        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getUsername());
        try {
            repository.save(toExistingDbEntity(entity));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new HttpConflictException(ex.getMessage());
        }
    }

    @Override
    public void deletePatient(@PathVariable String patientId) {
        // If not found just return ok to behave idempotent...
        LOG.debug("Delete by id: {}", patientId);
        repository.delete(patientId);
    }

    @Override
    public ResponseEntity<List<Id>> getAssignedInStudies(@PathVariable String patientId) {

        LOG.debug("getAssignedDoctors to study with id: {}", patientId);

        List<Id> studyIds = patientDoctorStudyRepository.findByPatientId(patientId).stream()
            .map(s -> new Id().withId(s.getStudy().getId()))
            .collect(Collectors.toList());

        LOG.debug("getAssignedInStudies found: #{}", studyIds.size());

        return util.createOkResponse(studyIds);
    }

    @Override
    public void addMeasurementToPatientInStudy(@PathVariable String patientId, @PathVariable String studyId, @RequestBody Measurement measurement) {
        LOG.debug("Add measurement to patient with id {} for study with id: {}", patientId, studyId);
        measurementRepository.save(toNewDbMeasurementEntity(findByPatientIdAndStudyId(patientId, studyId), measurement));
    }

    @Override
    public ResponseEntity<List<Measurement>> getPatientInStudyMeasurements(@PathVariable String patientId, @PathVariable String studyId) {

        LOG.debug("getPatientInStudyMeasurements for patient with id {} for study with id: {}", patientId, studyId);

        List<Measurement> measurements = findByPatientIdAndStudyId(patientId, studyId).getMeasurements().stream()
            .map(m -> toApiMeasurementEntity(m))
            .collect(Collectors.toList());

        LOG.debug("getPatientInStudyMeasurements found: #{}", measurements.size());

        return util.createOkResponse(measurements);
    }

    @Override
    public void deleteMeasurmentFromPatientInStudy(@PathVariable String patientId, @PathVariable String studyId, @PathVariable String measurementId) {
        LOG.debug("Delete measurement with id: #{}", measurementId);
        measurementRepository.delete(measurementId);
    }

    private PatientDoctorStudyEntity findByPatientIdAndStudyId(String patientId, String studyId) {
        // FIXME. What to do if there are >1 study assigned to one and the same patient? (indicates inconsistent data...)
        List<PatientDoctorStudyEntity> list = patientDoctorStudyRepository.findByPatientIdAndStudyId(patientId, studyId);
        return (list.size() == 0) ? null : list.get(0);
    }

    private List<Patient> findAll(String orderBy, Sortable.Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Sortable.Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        org.springframework.data.domain.Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

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
