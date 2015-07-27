package com.az.ip.api;

import static java.util.Collections.singletonList;

import com.az.ip.api.model.*;
import com.az.ip.api.model.Error;
import com.az.ip.api.persistence.jpa.JpaStudy;
import com.az.ip.api.persistence.jpa.StudyRepository;
import com.az.ip.api.resource.Studies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by magnus on 11/07/15.
 */
@Path("studies")
public class StudyResource implements Studies {

    private static final Logger LOG = LoggerFactory.getLogger(StudyResource.class);

    private static final List<String> ORDER_FIELDS  = Arrays.asList(new String[]{"name"});
    private static final String DEFAULT_ORDER_FIELD = "name";

    @Inject
    StudyRepository repository;

    @Override
    @GET
    @Produces({
            "application/json"
    })
    public GetStudiesResponse getStudies(
            @QueryParam("name")                       String name,
            @QueryParam("orderBy")                    String orderBy,
            @QueryParam("order") @DefaultValue("asc") Studies.Order order,
            @QueryParam("page")  @DefaultValue("0")   long page,
            @QueryParam("size")  @DefaultValue("20")  long size) {

        // Find by name?
        if (name != null) {
            LOG.debug("findByName, name: {}", name);
            JpaStudy entity = repository.findByName(name);
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
            repository.save(toNewDbEntity(entity));
            return PostStudiesResponse.withOK();
        } catch (RuntimeException ex) {
            // TODO: Add checks for common erros such as duplicate detectien and improve error message!
            LOG.error("postPatient request failed, exception: [{}], cause: []{}", ex, ex.getCause());
            return PostStudiesResponse.withJsonConflict(new com.az.ip.api.model.Error().withCode(-1).withMessage(ex.getMessage()));
        }
    }

    @Override
    public GetStudiesByIdResponse getStudiesById(String id) throws Exception {
        LOG.debug("Get by id: {}", id);
        JpaStudy entity = repository.findOne(id);

        if (entity == null) {
            LOG.debug("Entity with id: {} was not found", id);
            return GetStudiesByIdResponse.withNotFound();

        } else {
            LOG.debug("Found entity with id: {} and name: {}", entity.getId(), entity.getName());
            return GetStudiesByIdResponse.withJsonOK(toApiEntity(entity));
        }
    }

    @Override
    public PutStudiesByIdResponse putStudiesById(String id, String accessToken, Study entity) throws Exception {

        // TODO: What to do if not found??? Upsert or error???
        LOG.debug("Update entity: {}, {}, {}", entity.getId(), entity.getVersion(), entity.getName());
        repository.save(toExistingDbEntity(entity));

        return PutStudiesByIdResponse.withOK();
    }

    @Override
    public DeleteStudiesByIdResponse deleteStudiesById(String id, String accessToken) throws Exception {

        // If not found just return ok to behave idempotent...
        repository.delete(id);

        return DeleteStudiesByIdResponse.withOK();
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

    private Study toApiEntity(JpaStudy entity) {
        return new Study()
            .withId(entity.getId())
            .withVersion(entity.getVersion())
            .withName(entity.getName())
            .withDescription(entity.getDescription())
            .withStartdate(entity.getStartdate())
            .withEnddate(entity.getEnddate());
    }

    private JpaStudy toNewDbEntity(Study entity) {
        return new JpaStudy(
            entity.getName(),
            entity.getDescription(),
            entity.getStartdate(),
            entity.getEnddate()
        );
    }

    private JpaStudy toExistingDbEntity(Study entity) {
        return new JpaStudy(
            entity.getId(),
            entity.getVersion(),
            entity.getName(),
            entity.getDescription(),
            entity.getStartdate(),
            entity.getEnddate()
        );
    }
}