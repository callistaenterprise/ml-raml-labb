package com.az.ip.api.services.api;

import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.model.Measurement;
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
@RequestMapping("/api/studies")
@Api(
    value       = "studies",
    description = "Operations on studies")
public interface StudiesResource {

    /**
     * Find studies
     *
     * @param name
     * @param sort
     * @param page
     * @return
     */
    @RequestMapping(
        method   = GET,
        produces = "application/json")
    @ApiOperation(
        value    = "Find studies, optionally filter by name",
        notes    = "Support standard paging and sorting, orderBy fields: name",
        response = Study[].class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Invalid parameters in request, see error message in body for more information")
    })
    ResponseEntity<List<Study>> findStudies(
        @RequestParam String name,
        @ModelAttribute Sortable sort,
        @ModelAttribute Pageable page);


    /**
     * Create a new study
     *
     * @param entity
     * @return
     */
    @RequestMapping(
        method   = POST,
        consumes = "application/json",
        produces = "application/json")
    @ApiOperation(
        value    = "Creates a new study",
        notes    = "Returns the new entity with its id and version set",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 409, message = "Duplicate error, see error message in body for more information")
    })
    ResponseEntity<Study> createStudy(@RequestBody Study entity);


    /**
     * Get a study by its id
     *
     * @param studyId
     * @return
     */
    @RequestMapping(
        value    = "/{studyId}",
        method   = GET,
        produces = "application/json")
    @ApiOperation(
        value    = "Get study by Id",
        notes    = "",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "The entity was not found")
    })
    ResponseEntity<Study> getStudy(@PathVariable String studyId);


    /**
     * Updates a study
     *
     * @param studyId
     * @param entity
     */
    @RequestMapping(
        value    = "/{studyId}",
        method   = PUT,
        consumes = "application/json")
    @ApiOperation(
        value    = "Updates a study",
        notes    = "The version number is used for optimistic locking, i.e. to detect if a concurrent update has been performed, returns a 422 error if the version field is old",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Unprocessable entity, the entity has been updated by someone else.")
    })
    void updateStudy(
        @PathVariable String studyId,
        @RequestBody Study entity);



    /**
     * Deletes a study
     *
     * @param studyId
     * @return
     */
    @RequestMapping(
        value    = "/{studyId}",
        method   = DELETE)
    @ApiOperation(
        value    = "Deletes a study",
        notes    = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown",
        response = Study.class)
    void deleteStudy(@PathVariable String studyId);


    /**
     * Get a ref to each doctor that is assigned to this study
     *
     * @param studyId
     * @return a list of doctorId-refs
     */
    @RequestMapping(
        value    = "/{studyId}/assignedDoctors",
        method   = GET,
        produces = "application/json")
    ResponseEntity<List<Id>> getAssignedDoctors(@PathVariable String studyId);


    /**
     * Assigns a doctor to this study
     *
     * @param studyId
     *
     * @param doctorIdRef
     *      e.g. { "id" : "1234567890" }
     *
     */
    @RequestMapping(
        value    = "/{studyId}/assignedDoctors",
        method   = POST,
        consumes = "application/json")
    void assignDoctorToStudy(
        @PathVariable String studyId,
        @RequestBody Id doctorIdRef);


    /**
     * Removes a doctor from the study
     *
     * @param studyId
     * @param doctorId
     *
     */
    @RequestMapping(
        value    = "/{studyId}/assignedDoctors/{doctorId}",
        method   = DELETE)
    @ApiOperation(
        value    = "Removes a doctor from the study",
        notes    = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown")
    void removeDoctorFromStudy(
        @PathVariable String studyId,
        @PathVariable String doctorId);


    /**
     * Get all measurements for this study
     *
     * @param studyId
     * @return
     */
    @RequestMapping(
        value    = "/{studyId}/measurements",
        method   = GET,
        produces = "application/json")
    ResponseEntity<List<Measurement>> getMeasurementsInStudy(@PathVariable String studyId);

}
