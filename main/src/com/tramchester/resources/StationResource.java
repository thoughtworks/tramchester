package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StationClosureDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.StationRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Api
@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource extends UsesRecentCookie implements APIResource {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);

    private final StationRepository stationRepository;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    public StationResource(StationRepository stationRepository,
                           UpdateRecentJourneys updateRecentJourneys, ObjectMapper mapper,
                           ProvidesNow providesNow,
                           StationLocations stationLocations, TramchesterConfig config) {
        super(updateRecentJourneys, providesNow, mapper);
        this.stationRepository = stationRepository;
        this.stationLocations = stationLocations;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/{id}")
    @ApiOperation(value = "Get station by id", response = LocationDTO.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response get(@PathParam("id") String text) {
        logger.info("Get station " + text);
        IdFor<Station> id = IdFor.createId(text);
        if (stationRepository.hasStationId(id)) {
            return Response.ok(new LocationDTO(stationRepository.getStationById(id))).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // TODO CACHE/304 based on version of the data
    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Get all stations, use /mode instead if possible", response = StationRefDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response getAll() {
        logger.info("Get all stations");

        Set<Station> allStations = stationRepository.getStations();

        List<StationRefDTO> results = toStationRefDTOList(allStations);

        return Response.ok(results).build();
    }

    // TODO CACHE/304 based on version of the data
    @GET
    @Timed
    @Path("/mode/{mode}")
    @ApiOperation(value = "Get all stations", response = StationRefDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response geByMode(@PathParam("mode") String rawMode) {
        logger.info("Get stations for transport mode: " + rawMode);

        try {
            TransportMode mode = TransportMode.valueOf(rawMode);
            Set<Station> matching = stationRepository.getStationsForMode(mode);
            List<StationRefDTO> results = toStationRefDTOList(matching);
            return Response.ok(results).build();
        }
        catch(IllegalArgumentException missing) {
            logger.warn("Unable to match transport mode " + rawMode, missing);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

    }

    @GET
    @Timed
    @Path("/near")
    @ApiOperation(value = "Get stations close to a given lat/lon", response = StationRefDTO.class, responseContainer = "List")
    @CacheControl(noCache = true)
    public Response getNear(@QueryParam("lat") double lat, @QueryParam("lon") double lon) {
        logger.info(format("Get stations near to %s,%s", lat, lon));
        LatLong latLong = new LatLong(lat,lon);

        List<Station> nearestStations = stationLocations.nearestStationsSorted(latLong, config.getNumOfNearestStopsToOffer(),
                config.getNearestStopRangeKM());

        List<StationRefDTO> results = toStationRefDTOList(nearestStations);

        return Response.ok(results).build();
    }

    @GET
    @Timed
    @Path("/recent")
    @ApiOperation(value = "Get recent stations based on supplied cookie", response = StationRefDTO.class, responseContainer = "List")
    @CacheControl(noCache = true)
    public Response getRecent(@CookieParam(TRAMCHESTER_RECENT) Cookie cookie) {
        logger.info(format("Get recent stations for cookie %s", cookie));

        RecentJourneys recentJourneys = recentFromCookie(cookie);

        Set<Station> recent = recentJourneys.stream().map(Timestamped::getId).
                filter(id -> stationRepository.hasStationId(IdFor.createId(id))).
                map(id -> stationRepository.getStationById(IdFor.createId(id))).
                collect(Collectors.toSet());

        List<StationRefDTO> results = toStationRefDTOList(recent);

        return Response.ok(results).build();
    }

    @NotNull
    public List<StationRefDTO> toStationRefDTOList(Collection<Station> stations) {
        return stations.stream().map(StationRefDTO::new).collect(Collectors.toList());
    }

    // TODO CACHE/304 based on version of the data
    @GET
    @Timed
    @Path("/closures")
    @ApiOperation(value = "Get closed stations", response = StationClosureDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 5, maxAgeUnit = TimeUnit.MINUTES)
    public Response getClosures() {
        List<StationClosure> closures = config.getStationClosures();

        logger.info("Get closed stations " + closures);

        List<StationClosureDTO> dtos = closures.stream().map(this::createClosureDTO).collect(Collectors.toList());
        return Response.ok(dtos).build();

    }

    private StationClosureDTO createClosureDTO(StationClosure stationClosure) {
        Station closedStation = stationRepository.getStationById(stationClosure.getStation());
        return new StationClosureDTO(stationClosure, closedStation);
    }

}
