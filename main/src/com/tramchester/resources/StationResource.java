package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.Timestamped;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationListDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithGroupDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Api
@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);

    private List<Station> allStationsSorted;
    private final SpatialService spatialService;
    private final ClosedStations closedStations;
    private final StationRepository stationRepository;
    private final MyLocationFactory locationFactory;
    private final ProximityGroups proximityGroups;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    public StationResource(StationRepository stationRepository, SpatialService spatialService,
                           ClosedStations closedStations, UpdateRecentJourneys updateRecentJourneys, ObjectMapper mapper,
                           MyLocationFactory locationFactory, ProvidesNow providesNow, ProximityGroups proximityGroups,
                           StationLocations stationLocations, TramchesterConfig config) {
        super(updateRecentJourneys, providesNow, mapper);
        this.spatialService = spatialService;
        this.closedStations = closedStations;
        this.stationRepository = stationRepository;
        this.locationFactory = locationFactory;
        this.proximityGroups = proximityGroups;
        this.stationLocations = stationLocations;
        this.config = config;
        allStationsSorted = new ArrayList<>();
    }

    @GET
    @Timed
    @Path("/{id}")
    @ApiOperation(value = "Get station by id", response = StationDTO.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response get(@PathParam("id") String id) {
        logger.info("Get station " + id);
        if (stationRepository.hasStationId(id)) {
            return Response.ok(new LocationDTO(stationRepository.getStationById(id))).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all stations", response = StationListDTO.class)
    @CacheControl(noCache = true)
    public Response getAll(@CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) {
        logger.info("Get all stations with cookie " + tranchesterRecent);

        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        List<StationRefWithGroupDTO> displayStations = getStations().stream().
                filter(station -> !recentJourneys.containsStationId(station.getId())).
                map(station -> new StationRefWithGroupDTO(station, ProximityGroups.STOPS)).
                collect(Collectors.toList());

        recentJourneys.getRecentIds().forEach(recent -> {
            logger.info("Adding recent station to list " + recent);
            String recentId = recent.getId();
            if (stationRepository.hasStationId(recentId)) {
                displayStations.add(new StationRefWithGroupDTO(stationRepository.getStationById(recentId), ProximityGroups.RECENT));
            } else {
                logger.warn("Unrecognised recent stationid " + recentId);
            }
        });

        return Response.ok(new StationListDTO(displayStations, proximityGroups.getGroups())).build();
    }

    @GET
    @Timed
    @Path("/update")
    @ApiOperation(value = "Get updates to station list", response = StationListDTO.class)
    @CacheControl(noCache = true)
    public Response getUpdatesToList(@CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) {
        logger.info("Get updates to stations with cookie " + tranchesterRecent);

        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        List<StationRefWithGroupDTO> displayStations = new LinkedList<>();
        recentJourneys.getRecentIds().forEach(recent -> {
            logger.info("Adding recent station to list " + recent);
            String recentId = recent.getId();
            if (stationRepository.hasStationId(recentId)) {
                displayStations.add(new StationRefWithGroupDTO(stationRepository.getStationById(recentId), ProximityGroups.RECENT));
            } else {
                logger.warn("Unrecognised recent stationid " + recentId);
            }
        });

        return Response.ok(new StationListDTO(displayStations, Collections.singletonList(ProximityGroups.RECENT))).build();
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close stations", response = StationListDTO.class)
    @CacheControl(noCache = true)
    public Response getNearest(@PathParam("lat") double lat, @PathParam("lon") double lon,
                               @CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) {
        logger.info(format("Get station at %s,%s with recentcookie '%s'", lat, lon, tranchesterRecent));

        LatLong latLong = new LatLong(lat,lon);
        List<StationRefWithGroupDTO> orderedStations = spatialService.reorderNearestStations(latLong, getStations());

        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        recentJourneys.getRecentIds().forEach(recent -> {
            String recentId = recent.getId();
            if (stationRepository.hasStationId(recentId)) {
                Station recentStation = stationRepository.getStationById(recentId);
                orderedStations.remove(new StationRefWithGroupDTO(recentStation, ProximityGroups.STOPS));
                orderedStations.add(0, new StationRefWithGroupDTO(recentStation, ProximityGroups.RECENT));
            } else {
                logger.warn("Unrecognised recent station id: " + recentId);
            }
        });

        MyLocation myLocation = locationFactory.create(latLong);
        orderedStations.add(0, new StationRefWithGroupDTO(myLocation, ProximityGroups.MY_LOCATION));

        return Response.ok(new StationListDTO(orderedStations, proximityGroups.getGroups())).build();
    }

    @GET
    @Timed
    @Path("/update/{lat}/{lon}")
    @ApiOperation(value = "Get updates to station list", response = StationListDTO.class)
    @CacheControl(noCache = true)
    public Response getNearestUpdatedList(@PathParam("lat") double lat, @PathParam("lon") double lon,
                               @CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) {
        logger.info(format("Get station at %s,%s with recentcookie '%s'", lat, lon, tranchesterRecent));

        LatLong latLong = new LatLong(lat,lon);
        List<Station> nearestStations = stationLocations.getNearestStationsTo(latLong,
                config.getNumOfNearestStops(), config.getNearestStopRangeKM());
        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        Set<String> recentIds = recentJourneys.getRecentIds().stream().map(Timestamped::getId).collect(Collectors.toSet());

        List<StationRefWithGroupDTO> results =  new ArrayList<>();

        // add nearby not in recents list
        nearestStations.forEach(near -> {
            if (!recentIds.contains(near.getId())) {
                results.add(new StationRefWithGroupDTO(near, ProximityGroups.NEAREST_STOPS));
            }
        });

        // add recents
        recentJourneys.getRecentIds().forEach(recent -> {
            String recentId = recent.getId();
            if (stationRepository.hasStationId(recentId)) {
                Station recentStation = stationRepository.getStationById(recentId);
                results.add(0, new StationRefWithGroupDTO(recentStation, ProximityGroups.RECENT));
            } else {
                logger.warn("Unrecognised recent station id: " + recentId);
            }
        });

        List<ProximityGroup> groups = Arrays.asList(ProximityGroups.RECENT, ProximityGroups.NEAREST_STOPS);
        return Response.ok(new StationListDTO(results, groups)).build();
    }

    private List<Station> getStations() {
        if (allStationsSorted.isEmpty()) {
            Set<Station> rawList = stationRepository.getStations();
            allStationsSorted = rawList.stream().
                    sorted(Comparator.comparing(Station::getName)).
                    filter(station -> !closedStations.contains(station.getName())).
                    collect(Collectors.toList());
        }
        return allStationsSorted;
    }

}
