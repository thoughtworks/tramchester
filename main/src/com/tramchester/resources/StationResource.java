package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.StationClosureDTO;
import com.tramchester.domain.presentation.DTO.StationRefDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.RecentJourneys;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.CompositeStationRepository;
import com.tramchester.repository.DataSourceRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.StationRepositoryPublic;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Api
@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource extends UsesRecentCookie  {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);

    private final StationRepositoryPublic stationRepository;
    private final DataSourceRepository dataSourceRepository;
    private final StationLocations stationLocations;
    private final TramchesterConfig config;

    @Inject
    public StationResource(CompositeStationRepository stationRepository,
                           UpdateRecentJourneys updateRecentJourneys, ObjectMapper mapper,
                           ProvidesNow providesNow,
                           DataSourceRepository dataSourceRepository, StationLocations stationLocations, TramchesterConfig config) {
        super(updateRecentJourneys, providesNow, mapper);
        this.stationRepository = stationRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.stationLocations = stationLocations;
        this.config = config;
    }

    @GET
    @Timed
    @Path("/{id}")
    @ApiOperation(value = "Get station by id", response = LocationDTO.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public Response get(@PathParam("id") String text) {
        logger.info("Get station by id: " + text);
        
        StringIdFor<Station> id = StringIdFor.createId(text);
        guardForStationNotExisting(stationRepository, id);

        return Response.ok(new LocationDTO(stationRepository.getStationById(id))).build();
    }

    @GET
    @Timed
    @Path("/mode/{mode}")
    @ApiOperation(value = "Get all stations for transport mode", response = StationRefDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS, isPrivate = false)
    public Response getByMode(@PathParam("mode") String rawMode, @Context Request request) {
        logger.info("Get stations for transport mode: " + rawMode);

        try {
            TransportMode mode = TransportMode.valueOf(rawMode);
            Set<Station> matching = stationRepository.getStationsForMode(mode);

            LocalDateTime modTime = dataSourceRepository.getNewestModTimeFor(mode);
            Date date = Date.from(modTime.toInstant(ZoneOffset.UTC));

            Response.ResponseBuilder builder = request.evaluatePreconditions(date);

            if (builder==null) {
                logger.debug("modified");
                List<StationRefDTO> results = toStationRefDTOList(matching);
                if (results.isEmpty()) {
                    logger.info("No stations found for " + mode.name());
                }
                return Response.ok(results).lastModified(date).build();
            } else {
                logger.debug("Not modified");
                return builder.build();
            }
        }
        catch(IllegalArgumentException missing) {
            logger.warn("Unable to match transport mode string " + rawMode, missing);
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
                filter(id -> stationRepository.hasStationId(StringIdFor.createId(id))).
                map(id -> stationRepository.getStationById(StringIdFor.createId(id))).
                collect(Collectors.toSet());

        List<StationRefDTO> results = toStationRefDTOList(recent);

        return Response.ok(results).build();
    }

    @NotNull
    private List<StationRefDTO> toStationRefDTOList(Collection<Station> stations) {
        return stations.stream().
                map(StationRefDTO::new).
                // sort server side is here as an optimisation for front end sorting time
                sorted(Comparator.comparing(dto -> dto.getName().toLowerCase())).
                collect(Collectors.toList());
    }

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
