package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.search.FindStationLinks;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Api
@Path("/links")
@Produces(MediaType.APPLICATION_JSON)
public class StationLinksResource {
    private static final Logger logger = LoggerFactory.getLogger(StationLinksResource.class);

    private final FindStationLinks findStationLinks;

    @Inject
    public StationLinksResource(FindStationLinks findStationLinks) {
        this.findStationLinks = findStationLinks;
    }

    @GET
    @Timed
    @Path("/mode/{mode}")
    @ApiOperation(value = "Get all pairs of station links for given transport mode", response = StationRefDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAll(@PathParam("mode") String rawMode) {
        logger.info("Get station links for " + rawMode);

        TransportMode mode = TransportMode.valueOf(rawMode);
        Set<FindStationLinks.StationLink> links = findStationLinks.findFor(mode);

        List<StationLinkDTO> results = links.stream().map(this::create).collect(Collectors.toList());

        return Response.ok(results).build();
    }

    private StationLinkDTO create(FindStationLinks.StationLink link) {
        return new StationLinkDTO(new StationRefWithPosition(link.getBegin()), new StationRefWithPosition(link.getEnd()));
    }

}
