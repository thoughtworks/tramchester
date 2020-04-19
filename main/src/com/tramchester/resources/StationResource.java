package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.*;
import com.tramchester.domain.places.MyLocation;
import com.tramchester.domain.places.MyLocationFactory;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.*;
import com.tramchester.domain.presentation.DTO.factory.StationDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportDataFromFiles;
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
    private final ProvidesNotes providesNotes;
    private final MyLocationFactory locationFactory;
    private final StationDTOFactory stationDTOFactory;
    private final ProvidesNow providesNow;

    public StationResource(TransportDataFromFiles transportData, SpatialService spatialService,
                           ClosedStations closedStations,
                           UpdateRecentJourneys updateRecentJourneys,
                           ObjectMapper mapper,
                           ProvidesNotes providesNotes,
                           MyLocationFactory locationFactory, StationDTOFactory stationDTOFactory, ProvidesNow providesNow) {
        super(updateRecentJourneys, providesNow, mapper);
        this.spatialService = spatialService;
        this.closedStations = closedStations;
        this.stationRepository = transportData;
        this.providesNotes = providesNotes;
        this.locationFactory = locationFactory;
        this.stationDTOFactory = stationDTOFactory;
        this.providesNow = providesNow;
        allStationsSorted = new ArrayList<>();
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all stations", response = StationListDTO.class)
    @CacheControl(noCache = true)
    public Response getAll(@CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) {
        logger.info("Get all stations with cookie " + tranchesterRecent);

        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        List<StationDTO> displayStations = getStations().stream().
                filter(station -> !recentJourneys.containsStationId(station.getId())).
                map(station -> new StationDTO(station, ProximityGroup.ALL)).
                collect(Collectors.toList());

        recentJourneys.getRecentIds().forEach(recent -> {
            logger.info("Adding recent station to list " + recent);
            String recentId = recent.getId();
            if (stationRepository.hasStationId(recentId)) {
                displayStations.add(new StationDTO(stationRepository.getStation(recentId), ProximityGroup.RECENT));
            } else {
                logger.warn("Unrecognised recent stationid " + recentId);
            }
        });

        return Response.ok(new StationListDTO(displayStations, ProximityGroup.ALL_GROUPS)).build();
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

    @GET
    @Timed
    @Path("/{id}")
    @ApiOperation(value = "Get station by id", response = StationDTO.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response get(@PathParam("id") String id) {
        logger.info("Get station " + id);
        if (stationRepository.hasStationId(id)) {
            return Response.ok(new LocationDTO(stationRepository.getStation(id))).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Timed
    @Path("/live/{id}")
    @ApiOperation(value = "Get station by id enriched with live data", response = StationDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getLive(@PathParam("id") String id) {
        logger.info("Get station " + id);
        TramServiceDate queryDate = new TramServiceDate(providesNow.getDate());

        if (stationRepository.hasStationId(id)) {
            LocationDTO locationDTO = stationDTOFactory.build(stationRepository.getStation(id), queryDate, providesNow.getNow());
            return Response.ok(locationDTO).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Timed
    @Path("/live/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close stations enriched with live data", response = StationListDTO.class)
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestLive(@PathParam("lat") double lat, @PathParam("lon") double lon) {

        LatLong latLong = new LatLong(lat,lon);
        List<Station> stations = spatialService.getNearestStations(latLong);

        TramServiceDate queryDate = new TramServiceDate(providesNow.getDate());
        List<String> notes = providesNotes.createNotesForStations(stations, queryDate, providesNow.getNow());

        List<StationDTO> stationsDTO = new ArrayList<>();
        stations.forEach(station -> stationsDTO.add(stationDTOFactory.build(station, queryDate, providesNow.getNow())));

        return Response.ok(new StationListDTO(stationsDTO, notes, ProximityGroup.ALL_GROUPS)).build();
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
        List<StationDTO> orderedStations = spatialService.reorderNearestStations(latLong, getStations());

        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        recentJourneys.getRecentIds().forEach(recent -> {
            String recentId = recent.getId();
            if (stationRepository.hasStationId(recentId)) {
                Station recentStation = stationRepository.getStation(recentId);
                orderedStations.remove(new StationDTO(recentStation, ProximityGroup.ALL));
                orderedStations.add(0, new StationDTO(recentStation, ProximityGroup.RECENT));
            } else {
                logger.warn("Unrecognised recent station id: " + recentId);
            }
        });

        MyLocation myLocation = locationFactory.create(latLong);
        orderedStations.add(0, new StationDTO(myLocation, ProximityGroup.MY_LOCATION));

        return Response.ok(new StationListDTO(orderedStations,ProximityGroup.ALL_GROUPS)).build();
    }

}
