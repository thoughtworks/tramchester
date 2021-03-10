package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.StationPair;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;
import com.tramchester.graph.search.FindStationLinks;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.Tram;

@Api
@Path("/links")
@Produces(MediaType.APPLICATION_JSON)
public class StationLinksResource {
    private static final Logger logger = LoggerFactory.getLogger(StationLinksResource.class);

    private final FindStationLinks findStationLinks;
    private final TramchesterConfig config;

    @Inject
    public StationLinksResource(FindStationLinks findStationLinks, TramchesterConfig config) {
        this.findStationLinks = findStationLinks;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Get all pairs of station links for given transport mode", response = StationRefDTO.class,
            responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAll() {
        logger.info("Get station links");

        ArrayList<StationLink> allLinks = new ArrayList<>();

        config.getTransportModes().forEach(transportMode -> {
            Set<StationLink> links = findStationLinks.findFor(transportMode);
            allLinks.addAll(links);
        });

        List<StationLinkDTO> results = allLinks.stream().map(this::create).collect(Collectors.toList());

        return Response.ok(results).build();
    }

    private StationLinkDTO create(StationLink link) {
        StationPair stationPair = link.getStations();
        return new StationLinkDTO(new StationRefWithPosition(stationPair.getBegin()),
                new StationRefWithPosition(stationPair.getEnd()), Collections.singleton(Tram));
    }

}
