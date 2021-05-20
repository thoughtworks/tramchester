package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.presentation.DTO.BoxDTO;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.NeighboursRepository;
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

@Api
@Path("/links")
@Produces(MediaType.APPLICATION_JSON)
public class StationLinksResource {
    private static final Logger logger = LoggerFactory.getLogger(StationLinksResource.class);

    private final FindStationLinks findStationLinks;
    private final NeighboursRepository neighboursRepository;
    private final CompositeStationRepository compositeStationRepository;
    private final TramchesterConfig config;
    private final StationLocations stationLocations;

    @Inject
    public StationLinksResource(FindStationLinks findStationLinks, NeighboursRepository neighboursRepository,
                                CompositeStationRepository compositeStationRepository, TramchesterConfig config,
                                StationLocations stationLocations) {
        this.findStationLinks = findStationLinks;
        this.neighboursRepository = neighboursRepository;
        this.compositeStationRepository = compositeStationRepository;
        this.config = config;
        this.stationLocations = stationLocations;
    }

    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Get all pairs of station links for given transport mode", response = StationLinkDTO.class,
            responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAll() {
        logger.info("Get station links");

        ArrayList<StationLink> allLinks = new ArrayList<>();

        config.getTransportModes().forEach(transportMode -> {
            Set<StationLink> links = findStationLinks.findLinkedFor(transportMode);
            allLinks.addAll(links);
        });

        List<StationLinkDTO> results = allLinks.stream().
                filter(StationLink::hasValidLatlongs).
                map(StationLinkDTO::create).collect(Collectors.toList());

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/neighbours")
    @ApiOperation(value = "Get all pairs of neighbours", response = StationLinkDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getNeighbours() {
        logger.info("Get station neighbours");

        if (!config.getCreateNeighbours()) {
            logger.warn("Neighbours disabled");
            return Response.ok(Collections.<StationLinkDTO>emptyList()).build();
        }

        List<StationLink> allLinks = neighboursRepository.getAll();

        List<StationLinkDTO> results = allLinks.stream().
                filter(StationLink::hasValidLatlongs).
                map(StationLinkDTO::create).collect(Collectors.toList());

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/composites")
    @ApiOperation(value = "Get all pairs of composites (parent & child)", response = StationGroupDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getLinks() {
        logger.info("Get composite links");

        List<StationGroupDTO> groups = new ArrayList<>();
        config.getTransportModes().forEach(mode ->
                groups.addAll(compositeStationRepository.getCompositesFor(mode).stream().
                        map(StationGroupDTO::create).collect(Collectors.toSet())));

        return Response.ok(groups).build();

    }

    @GET
    @Timed
    @Path("/quadrants")
    @ApiOperation(value = "Get station location quadrants", response = BoxDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getQuadrants() {
        logger.info("Get quadrants");

        List<BoxDTO> quadrantDTOs = stationLocations.getQuadrants().stream().
                map(BoxDTO::new).collect(Collectors.toList());

        return Response.ok(quadrantDTOs).build();
    }


}
