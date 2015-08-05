package com.az.ip.api;

import com.az.ip.api.gen.model.Doctor;
import com.az.ip.api.gen.model.Error;
import com.az.ip.api.gen.model.Id;
import com.az.ip.api.gen.resource.DoctorsResource;
import com.az.ip.api.persistence.jpa.DoctorEntity;
import com.az.ip.api.persistence.jpa.DoctorRepository;
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
@Path("doctors")
public class DoctorsResourceImpl implements DoctorsResource {

    private static final Logger LOG = LoggerFactory.getLogger(DoctorsResourceImpl.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"username", "firstName", "lastName"});
    private static final String DEFAULT_ORDER_FIELD = "username";

    @Inject
    DoctorRepository repository;

    /**
     * Search doctors
     *
     * TODO: We need to retain the JAX-RS annotiation from the Doctors-interface, don't know why...
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
    public GetDoctorsResponse getDoctors(
        @QueryParam("username")                   String username,
        @QueryParam("orderBy")                    String orderBy,
        @QueryParam("order") @DefaultValue("asc") Order order,
        @QueryParam("page")  @DefaultValue("0")   long page,
        @QueryParam("size")  @DefaultValue("10")  long size) {

        // Find by name?
        if (username != null) {
            LOG.debug("findByName, name: {}", username);
            DoctorEntity entity = repository.findByUsername(username);
            return GetDoctorsResponse.withJsonOK((entity == null) ? new ArrayList<>() : singletonList(toApiEntity(entity)));
        }

        LOG.debug("find, orderBy: {}, order: {}, page: {}, size: {}", orderBy, order, page, size);

        // TODO: Can we make this a @DefaultValue instead?
        if (orderBy == null) orderBy = DEFAULT_ORDER_FIELD;

        // The orderBy field has to be part of the list of allowed fields
        if (!ORDER_FIELDS.contains(orderBy)) {
            String errMsg = "Order field [" + orderBy + "] must be on of: " + ORDER_FIELDS;
            LOG.error("getDoctors request failed: " + errMsg);
            return GetDoctorsResponse.withJsonUnprocessableEntity(new Error().withCode(-1).withMessage(errMsg));
        }

        return GetDoctorsResponse.withJsonOK(findAll(orderBy, order, page, size));
    }

    /**
     * Create a new doctor
     *
     * @param accessToken
     *     The access token provided by the authentication application e.g. AABBCCDD
     * @param entity
     *      e.g. {
     *       "username":"D1",
     *       "firstname":"F1",
     *       "lastname":"L1",
     *       "weight":100,
     *       "height":200
     * @return
     * @throws Exception
     */
    @Override
    public PostDoctorsResponse postDoctors(String accessToken, Doctor entity) throws Exception {
        try {
            DoctorEntity newDoctor = repository.save(toNewDbEntity(entity));
            return PostDoctorsResponse.withJsonOK(toApiEntity(newDoctor));

        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("postDoctor request failed, exception: [{}], cause: []{}", ex, ex.getCause());
            return PostDoctorsResponse.withJsonConflict(new Error().withCode(-1).withMessage(ex.getMessage()));
        }
    }

    @Override
    public GetDoctorsByIdResponse getDoctorsById(String id) throws Exception {
        LOG.debug("Get by id: {}", id);
        DoctorEntity entity = repository.findOne(id);

        if (entity == null) {
            LOG.debug("Entity with id: {} was not found", id);
            return GetDoctorsByIdResponse.withNotFound();

        } else {
            LOG.debug("Found entity with id: {} and username: {}", entity.getId(), entity.getUsername());
            return GetDoctorsByIdResponse.withJsonOK(toApiEntity(entity));
        }
    }

    @Override
    public PutDoctorsByIdResponse putDoctorsById(String id, String accessToken, Doctor entity) throws Exception {

        // TODO: What to do if not found??? Upsert or error???
        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getUsername());
        repository.save(toExistingDbEntity(entity));

        return PutDoctorsByIdResponse.withOK();
    }

    @Override
    public DeleteDoctorsByIdResponse deleteDoctorsById(String id, String accessToken) throws Exception {

        // If not found just return ok to behave idempotent...
        repository.delete(id);

        return DeleteDoctorsByIdResponse.withOK();
    }

    @Override
    public GetDoctorsByIdAssignedInStudiesResponse getDoctorsByIdAssignedInStudies(String id) throws Exception {
        DoctorEntity entity = repository.findOne(id);
        List<Id> studyIds = entity.getAssigendInStudies().stream().map(s -> new Id().withId(s.getId())).collect(Collectors.toList());
        return GetDoctorsByIdAssignedInStudiesResponse.withJsonOK(studyIds);
    }

    private List<Doctor> findAll(String orderBy, Order order, long page, long size) {

        Sort.Direction sortOrder = (order == Order.asc) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = new Sort(new Sort.Order(sortOrder, orderBy));

        Pageable pageable = (size == -1) ? null : new PageRequest((int)page, (int)size, sort);

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
