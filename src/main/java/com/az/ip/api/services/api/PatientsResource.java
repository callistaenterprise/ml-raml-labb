package com.az.ip.api.services.api;

import com.az.ip.api.gen.model.*;
import com.az.ip.api.services.model.Pageable;
import com.az.ip.api.services.model.Sortable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Created by magnus on 21/08/15.
 */
@RequestMapping("/api/patients")
@Api(
    value       = "patients",
    description = "Operations on patients")
public interface PatientsResource {

    /**
     * Find patients
     *
     * @param username
     * @param sort
     * @param page
     * @return
     */
    @RequestMapping(
        method   = GET,
        produces = "application/json")
    @ApiOperation(
        value    = "Find patients, optionally filter by username",
        notes    = "Support standard paging and sorting, orderBy fields: username, firstName, lastName",
        response = Patient[].class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Invalid parameters in request, see error message in body for more information")
    })
    ResponseEntity<List<Patient>> findPatients(
        @RequestParam String username,
        @ModelAttribute Sortable sort,
        @ModelAttribute Pageable page);


    /**
     * Create a new patient
     *
     * @param entity
     * @return
     */
    @RequestMapping(
        method   = POST,
        consumes = "application/json",
        produces = "application/json")
    @ApiOperation(
        value    = "Creates a new patient",
        notes    = "Returns the new entity with its id and version set",
        response = Patient.class)
    @ApiResponses(value = {
        @ApiResponse(code = 409, message = "Duplicate error, see error message in body for more information")
    })
    ResponseEntity<Patient> createPatient(@RequestBody Patient entity);


    /**
     * Get a patient by its id
     *
     * @param doctorId
     * @return
     */
    @RequestMapping(
        value    = "/{patientId}",
        method   = GET,
        produces = "application/json")
    @ApiOperation(
        value    = "Get patient by Id",
        notes    = "",
        response = Patient.class)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "The entity was not found")
    })
    ResponseEntity<Patient> getPatient(@PathVariable String patientId);


    /**
     * Updates a patient
     *
     * @param patientId
     * @param entity
     */
    @RequestMapping(
        value    = "/{patientId}",
        method   = PUT,
        consumes = "application/json")
    @ApiOperation(
        value    = "Updates a Patient",
        notes    = "The version number is used for optimistic locking, i.e. to detect if a concurrent update has been performed, returns a 422 error if the version field is old",
        response = Patient.class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Unprocessable entity, the entity has been updated by someone else.")
    })
    void updatePatient(
        @PathVariable String patientId,
        @RequestBody Patient entity);

    /**
     * Deletes a Patient
     *
     * @param patientId
     * @return
     */
    @RequestMapping(
        value    = "/{patientId}",
        method   = DELETE)
    @ApiOperation(
        value    = "Deletes a patient",
        notes    = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown",
        response = Patient.class)
    void deleteStudy(@PathVariable String patientId);


    /* -------------- */

    /**
     * Get a ref to each study that patient is assigned to
     *
     * @param patientId
     * @return a list of studyId-refs
     */
    @RequestMapping(
        value    = "/{patientId}/studies",
        method   = GET,
        produces = "application/json")
    ResponseEntity<List<Id>> getAssignedInStudies(@PathVariable String patientId);

    /**
     * Add a measurement to a patient
     *
     * @param patientId
     * @param studyId
     * @param measurement
     *
     */
    @RequestMapping(
        value    = "/{patientId}/studies/{studyId}/measurements",
        method   = POST,
        consumes = "application/json")
    void addMeasurementToPatientInStudy(
        @PathVariable String patientId,
        @PathVariable String studyId,
        @RequestBody Measurement measurement);


    /**
     * Get all measurements for a patient in a study
     *
     * @param patientId
     * @param studyId
     * @return a list of measurements
     */
    @RequestMapping(
        value    = "/{patientId}/studies/{studyId}/measurements",
        method   = GET,
        produces = "application/json")
    ResponseEntity<List<Measurement>> getPatientInStudyMeasurements(
        @PathVariable String patientId,
        @PathVariable String studyId);



    /**
     * Deletes a measurement from a patient in a study
     *
     * @param patientId
     * @param studyId
     * @param measurementId
     *
     */
    @RequestMapping(
        value    = "/{patientId}/studies/{studyId}/measurements/{measurementId}",
        method   = DELETE)
    @ApiOperation(
        value    = "Deletes a measurement from a patient in a study",
        notes    = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown")
    void deleteMeasuremntFromPatientInStudy(
        @PathVariable String patientId,
        @PathVariable String studyId,
        @PathVariable String measurementId);
}
