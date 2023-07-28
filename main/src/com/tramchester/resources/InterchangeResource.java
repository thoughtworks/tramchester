package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.repository.InterchangeRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/interchanges")
@Produces(MediaType.APPLICATION_JSON)
public class InterchangeResource implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeResource.class);

    private final InterchangeRepository interchangeRepository;

    @Inject
    public InterchangeResource(InterchangeRepository interchangeRepository) {
        this.interchangeRepository = interchangeRepository;
    }

    @GET
    @Timed
    @Path("/all")
    @Operation(description = "Get all interchanges")
    @ApiResponse(content = @Content(array = @ArraySchema(uniqueItems = true, schema = @Schema(implementation = String.class))))
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@Context Request request) {
        logger.info("Get all interchange id's");

        List<IdForDTO> stationIds = interchangeRepository.getAllInterchanges().stream().
                map(interchangeStation -> IdForDTO.createFor(interchangeStation.getStation())).collect(Collectors.toList());

        return Response.ok(stationIds).build();
    }



}
