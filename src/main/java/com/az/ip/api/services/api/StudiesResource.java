package com.az.ip.api.services.api;

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
    value = "studies",
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
        method = GET, produces="application/json")
    @ApiOperation(
        value = "Find studies, optionally filter by name",
        notes = "Support standard paging and sorting, orderBy fields: name",
        response = Study[].class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Invalid parameters in request, see error message in body for more information")
    })
    ResponseEntity<List<Study>> findStudies(
        @RequestParam("name") String name,
        @ModelAttribute Sortable sort,
        @ModelAttribute Pageable page);


    /**
     * Create a new study
     *
     * @param entity
     * @return
     */
    @RequestMapping(
        method = POST, consumes = "application/json", produces="application/json")
    @ApiOperation(
        value = "Creates a new study",
        notes = "Returns the new entity with its id and version set",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 409, message = "Duplicate error, see error message in body for more information")
    })
    ResponseEntity<Study> createStudy(
        @RequestBody Study entity);


    /**
     * Get a study by its id
     *
     * @param id
     * @return
     */
    @RequestMapping(
        value = "/{id}", method = GET, produces="application/json")
    @ApiOperation(
        value = "Get study by Id",
        notes = "",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 404, message = "The entity was not found")
    })
    ResponseEntity<Study> getStudy(
        @PathVariable String id);

    /**
     * Updates a study
     *
     * @param entity
     * @return
     */
    @RequestMapping(
        value = "/{id}", method = PUT, consumes="application/json", produces="application/json")
    @ApiOperation(
        value = "Updates a study",
        notes = "The version number is used to detect if a concurrent update has been performed, returns a 422 error if the version field is old",
        response = Study.class)
    @ApiResponses(value = {
        @ApiResponse(code = 422, message = "Unprocessable entity, the entity has been updated by someone else.")
    })
    void updateStudy(
        @PathVariable String id,
        @RequestBody Study entity);

    /**
     * Deletes a study
     *
     * @param id
     * @return
     */
    @RequestMapping(
        value = "/{id}", method = DELETE, consumes="application/json")
    @ApiOperation(
        value = "Deletes a study",
        notes = "The delete is idempotent, i.e. if the entity already is deleted no error will be thrown",
        response = Study.class)
    void deleteStudy(
        @PathVariable String id);
}
