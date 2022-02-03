package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.presentation.DTO.AreaBoundaryDTO;
import com.tramchester.domain.presentation.DTO.BoxDTO;
import com.tramchester.domain.presentation.DTO.StationGroupDTO;
import com.tramchester.domain.presentation.DTO.StationLinkDTO;
import com.tramchester.domain.presentation.DTO.factory.DTOFactory;
import com.tramchester.geo.StationLocations;
import com.tramchester.graph.search.FindStationLinks;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationGroupsRepository;
import com.tramchester.repository.naptan.NaptanRespository;
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
@Path("/geo")
@Produces(MediaType.APPLICATION_JSON)
public class StationGeographyResource implements APIResource, GraphDatabaseDependencyMarker {
    private static final Logger logger = LoggerFactory.getLogger(StationGeographyResource.class);

    private final FindStationLinks findStationLinks;
    private final NeighboursRepository neighboursRepository;
    private final StationGroupsRepository stationGroupsRepository;
    private final TramchesterConfig config;
    private final StationLocations stationLocations;
    private final NaptanRespository naptanRespository;
    private final DTOFactory dtoFactory;

    @Inject
    public StationGeographyResource(FindStationLinks findStationLinks, NeighboursRepository neighboursRepository,
                                    StationGroupsRepository stationGroupsRepository, TramchesterConfig config,
                                    StationLocations stationLocations, NaptanRespository naptanRespository, DTOFactory dtoFactory) {
        this.naptanRespository = naptanRespository;
        this.dtoFactory = dtoFactory;
        this.findStationLinks = findStationLinks;
        this.neighboursRepository = neighboursRepository;
        this.stationGroupsRepository = stationGroupsRepository;
        this.config = config;
        this.stationLocations = stationLocations;
    }

    @GET
    @Timed
    @Path("/links")
    @ApiOperation(value = "Get pairs of station links for given transport mode", response = StationLinkDTO.class, responseContainer = "List")
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
                map(dtoFactory::createStationLinkDTO).
                collect(Collectors.toList());

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

        Set<StationLink> allLinks = neighboursRepository.getAll();

        List<StationLinkDTO> results = allLinks.stream().
                filter(StationLink::hasValidLatlongs).
                map(dtoFactory::createStationLinkDTO).collect(Collectors.toList());

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/areas")
    @ApiOperation(value = "List of boundaries for each area along with the area code", responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAreas() {
        logger.info("Get areas");

        Set<NaptanArea> areas = naptanRespository.getAreas();

        logger.info("Get boundaries for " + areas.size() + " areas");

        List<AreaBoundaryDTO> allBoundaries = areas.stream().
                filter(area -> stationLocations.hasStationsInArea(area.getId())).
                filter(area -> naptanRespository.getNumRecordsFor(area.getId()) > 1).
                map(area -> new AreaBoundaryDTO(stationLocations.getBoundaryFor(area.getId()), area))
                .collect(Collectors.toList());

        logger.info("Found " + allBoundaries.size() + " areas with boundaries");

        return Response.ok(allBoundaries).build();
    }


    @GET
    @Timed
    @Path("/groups")
    @ApiOperation(value = "Get all pairs of composites (parent & child)", response = StationGroupDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getLinks() {
        logger.info("Get composite links");

        List<StationGroupDTO> groups = new ArrayList<>();
        config.getTransportModes().forEach(mode ->
                groups.addAll(stationGroupsRepository.getStationGroupsFor(mode).stream().
                        map(dtoFactory::createStationGroupDTO).collect(Collectors.toSet())));

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

    @GET
    @Timed
    @Path("/bounds")
    @ApiOperation(value = "Get current geographical bounds in effect", response = BoxDTO.class)
    public Response getBounds() {
        logger.info("Get bounds");

        BoxDTO quadrantDTOs = new BoxDTO(config.getBounds());

        return Response.ok(quadrantDTOs).build();

    }


}
