package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationListDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.repository.LiveDataRepository;
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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Api
@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource extends UsesRecentCookie {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);

    private List<Station> allStationsSorted;
    private final SpatialService spatialService;
    private final ClosedStations closedStations;
    private final StationRepository stationRepository;
    private final LiveDataRepository liveDataRepository;
    private ProvidesNotes providesNotes;
    private final MyLocationFactory locationFactory;

    public StationResource(TransportDataFromFiles transportData, SpatialService spatialService,
                           ClosedStations closedStations,
                           UpdateRecentJourneys updateRecentJourneys,
                           ObjectMapper mapper,
                           LiveDataRepository liveDataRepository,
                           ProvidesNotes providesNotes,
                           MyLocationFactory locationFactory) {
        super(updateRecentJourneys, mapper);
        this.spatialService = spatialService;
        this.closedStations = closedStations;
        this.stationRepository = transportData;
        this.liveDataRepository = liveDataRepository;
        this.providesNotes = providesNotes;
        this.locationFactory = locationFactory;
        allStationsSorted = new ArrayList<>();
    }

    private LocalDateTime getLocalNow() {
        return ZonedDateTime.now(TramchesterConfig.TimeZone).toLocalDateTime();
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
            Optional<Station> recentStation = stationRepository.getStation(recent.getId());
            recentStation.ifPresent(station -> displayStations.add(new StationDTO(station, ProximityGroup.RECENT)));
        });

        return Response.ok(new StationListDTO(displayStations)).build();
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
        Optional<Station> station = stationRepository.getStation(id);
        if (station.isPresent()) {
            return Response.ok(new LocationDTO(station.get())).build();
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
        Optional<Station> station = stationRepository.getStation(id);
        if (station.isPresent()) {
            LocationDTO locationDTO = new LocationDTO(station.get());
            liveDataRepository.enrich(locationDTO, getLocalNow());
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

        LocalDateTime localNow = getLocalNow();

        LatLong latLong = new LatLong(lat,lon);
        List<StationDTO> stations = spatialService.getNearestStations(latLong);
        stations.forEach(station -> {
            liveDataRepository.enrich(station, localNow);
        });

        TramServiceDate queryDate = new TramServiceDate(localNow.toLocalDate());
        List<String> notes = providesNotes.createNotesForStations(stations, queryDate);

        return Response.ok(new StationListDTO(stations,notes)).build();
    }



    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close stations", response = StationListDTO.class)
    @CacheControl(noCache = true)
    public Response getNearest(@PathParam("lat") double lat, @PathParam("lon") double lon,
                               @CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) throws JsonProcessingException {
        logger.info(format("Get station at %s,%s with recentcookie '%s'", lat, lon, tranchesterRecent));

        LatLong latLong = new LatLong(lat,lon);
        List<StationDTO> orderedStations = spatialService.reorderNearestStations(latLong, getStations());

        RecentJourneys recentJourneys = recentFromCookie(tranchesterRecent);

        recentJourneys.getRecentIds().forEach(recent -> {
            Optional<Station> recentStation = stationRepository.getStation(recent.getId());
            recentStation.ifPresent(station -> {
                orderedStations.remove(new StationDTO(station, ProximityGroup.ALL));
                orderedStations.add(0, new StationDTO(station, ProximityGroup.RECENT));
            });
        });


        MyLocation myLocation = locationFactory.create(latLong);
        orderedStations.add(0, new StationDTO(myLocation, ProximityGroup.MY_LOCATION));

        return Response.ok(new StationListDTO(orderedStations)).build();
    }

}
