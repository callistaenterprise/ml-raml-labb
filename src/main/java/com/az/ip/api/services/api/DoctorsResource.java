package com.az.ip.api.services.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Study;
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
@RequestMapping("/api/doctors")
@Api(
    value       = "doctors",
    description = "Operations on doctors")
public interface DoctorsResource {

    /**
     * Find doctors
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
        value    = "Find doctors, optionally filter by username",
        notes    = "Support standard paging and sorting, orderBy fields: username, firstName, lastName",
        response = Doctor[].class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Invalid parameters in request, see error message in body for more information")
    })
    ResponseEntity<List<Doctor>> findDoctors(
        @RequestParam   String   username,
        @ModelAttribute Sortable sort,
        @ModelAttribute Pageable page);


    /**
     * Create a new doctor
     *
     * @param entity
     * @return
     */
    @RequestMapping(
        method   = POST,
        consumes = "application/json",
        produces = "application/json")
    @ApiOperation(
        value    = "Creates a new doctor",
        notes    = "Returns the new entity with its id and version set",
        response = Doctor.class)
    @ApiResponses(value = {
        @ApiResponse(code = 409, message = "Duplicate error, see error message in body for more information")
    })
    ResponseEntity<Doctor> createDoctor(@RequestBody Doctor entity);


    /**
     * Get a doctor by its id
     *
     * @param doctorId
     * @return
     */
    @RequestMapping(
        value    = "/{doctorId}",
        method   = GET,
        produces = "application/json")
    @ApiOperation(
        value    = "Get doctor by Id",
        notes    = "",
        response = Doctor.class)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "The entity was not found")
    })
    ResponseEntity<Doctor> getDoctor(@PathVariable String doctorId);


    /**
     * Updates a doctor
     *
     * @param doctorId
     * @param entity
     */
    @RequestMapping(
        value    = "/{doctorId}",
        method   = PUT,
        consumes = "application/json")
    @ApiOperation(
        value    = "Updates a doctor",
        notes    = "The version number is used for optimistic locking, i.e. to detect if a concurrent update has been performed, returns a 422 error if the version field is old",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Unprocessable entity, the entity has been updated by someone else.")
    })
    void updateDoctor(
        @PathVariable String doctorId,
        @RequestBody  Study entity);

    /**
     * Deletes a doctor
     *
     * @param doctorId
     * @return
     */
    @RequestMapping(
        value    = "/{doctorId}",
        method   = DELETE)
    @ApiOperation(
        value    = "Deletes a doctor",
        notes    = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown",
        response = Doctor.class)
    void deleteStudy(@PathVariable String doctorId);

    /**
     * Get a ref to each study that this doctor is assigned to
     *
     * @param doctorId
     * @return a list of studyId-refs
     */
    @RequestMapping(
        value    = "/{doctorId}/assignedInStudies",
        method   = GET,
        produces = "application/json")
    ResponseEntity<List<Id>> getAssignedInStudies(@PathVariable String doctorId);

    /**
     * Assigns a patient to this doctor in this study
     *
     * @param doctorId
     * @param studyId
     * @param patientIdRef
     *      e.g. { "id" : "1234567890" }
     *
     */
    @RequestMapping(
        value    = "/{doctorId}/assignedInStudies/{studyId}/patients",
        method   = POST,
        consumes = "application/json")
    void assignDoctorToStudy(
        @PathVariable String doctorId,
        @PathVariable String studyId,
        @RequestBody  Id patientIdRef);

    /**
     * Get a ref to each patient that this doctor have assigned to this study
     *
     * @param doctorId
     * @param studyId
     * @return a list of patientId-refs
     */
    @RequestMapping(
        value    = "/{doctorId}/assignedInStudies/{studyId}/patients",
        method   = GET,
        produces = "application/json")
    ResponseEntity<List<Id>> getPatientsAssignedByDoctorInStudy(
        @PathVariable String doctorId,
        @PathVariable String studyId);


    /**
     * Deletes a ref to a patient, i.e. the patient is no longer assigned to the study by this doctor
     *
     * @param doctorId
     * @param studyId
     * @param patientId
     *
     */
    @RequestMapping(
        value    = "/{doctorId}/assignedInStudies/{studyId}/patients/{patientId}",
        method   = DELETE)
    @ApiOperation(
        value    = "Deletes a ref to a patient, i.e. the patient is no longer assigned to the study by this doctor",
        notes    = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown")
    void removePatientAssignedByDoctorInStudy(
        @PathVariable String doctorId,
        @PathVariable String studyId,
        @PathVariable String patientId);
}
