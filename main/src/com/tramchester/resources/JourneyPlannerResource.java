package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.Journey;
import com.tramchester.domain.MyLocationFactory;
import com.tramchester.domain.Station;
import com.tramchester.domain.UpdateRecentJourneys;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.graph.RouteCalculatorArriveBy;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.repository.TransportData;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.String.format;

@Api
@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource extends UsesRecentCookie {

    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);

    private final TramchesterConfig config;
    private final LocationJourneyPlanner locToLocPlanner;
    private final RouteCalculator routeCalculator;
    private final RouteCalculatorArriveBy routeCalculatorArriveBy;
    private final JourneysMapper journeysMapper;
    private final ProvidesNotes providesNotes;
    private final GraphDatabase graphDatabaseService;
    private final TransportData transportData;

    public JourneyPlannerResource(RouteCalculator routeCalculator, JourneysMapper journeysMapper, TramchesterConfig config,
                                  LocationJourneyPlanner locToLocPlanner, UpdateRecentJourneys updateRecentJourneys,
                                  ObjectMapper objectMapper, RouteCalculatorArriveBy routeCalculatorArriveBy,
                                  ProvidesNotes providesNotes, GraphDatabase graphDatabaseService, TransportData transportData) {
        super(updateRecentJourneys, objectMapper);
        this.routeCalculator = routeCalculator;
        this.journeysMapper = journeysMapper;
        this.config = config;
        this.locToLocPlanner = locToLocPlanner;
        this.routeCalculatorArriveBy = routeCalculatorArriveBy;
        this.providesNotes = providesNotes;
        this.graphDatabaseService = graphDatabaseService;
        this.transportData = transportData;
    }

    @GET
    @Timed
    @ApiOperation(value = "Find quickest route", response = JourneyPlanRepresentation.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.MINUTES)
    public Response quickestRoute(@QueryParam("start") String startId,
                                  @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTimeText,
                                  @QueryParam("departureDate") String departureDate,
                                  @QueryParam("lat") @DefaultValue("0") String lat,
                                  @QueryParam("lon") @DefaultValue("0") String lon,
                                  @QueryParam("arriveby") @DefaultValue("false") String arriveByRaw,
                                  @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie){
        logger.info(format("Plan journey from %s to %s at %s on %s", startId, endId,departureTimeText, departureDate));

        LocalDate date = LocalDate.parse(departureDate); // TODO need formatter?
        TramServiceDate queryDate = new TramServiceDate(date);

        try {
            Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeText);
            if (maybeDepartureTime.isPresent()) {
                TramTime queryTime = maybeDepartureTime.get();

                boolean arriveBy = Boolean.parseBoolean(arriveByRaw);

                JourneyPlanRepresentation planRepresentation;
                try (Transaction tx = graphDatabaseService.beginTx() ) {
                    if (isWalking(startId)) {
                        LatLong latLong = decodeLatLong(lat, lon);
                        planRepresentation = createJourneyPlanStartsWithWalk(latLong, endId, queryDate, queryTime, arriveBy);
                    } else if (isWalking(endId)) {
                        LatLong latLong = decodeLatLong(lat, lon);
                        planRepresentation = createJourneyPlanEndsWithWalk(startId, latLong, queryDate, queryTime, arriveBy);
                    } else {
                        planRepresentation = createJourneyPlan(startId, endId, queryDate, queryTime, arriveBy);
                    }
                }

                if (planRepresentation.getJourneys().size()==0) {
                    logger.warn(format("No journeys found from %s to %s at %s on %s", startId, endId,departureTimeText, departureDate));
                }

                Response.ResponseBuilder responseBuilder = Response.ok(planRepresentation);
                responseBuilder.cookie(createRecentCookie(cookie, startId, endId));
                return responseBuilder.build();
            }
        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
        }

        return Response.serverError().build();
    }

    private boolean isWalking(@QueryParam("start") String startId) {
        return MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(startId);
    }

    private LatLong decodeLatLong(String lat, String lon) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
        return new LatLong(latitude,longitude);
    }

    private JourneyPlanRepresentation createJourneyPlanStartsWithWalk(LatLong latLong, String endId, TramServiceDate queryDate,
                                                                      TramTime queryTime, boolean arriveBy) {
        if (!transportData.hasStationId(endId)) {
            String msg = "Unable to find end station from id " + endId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }

        Station finalStation = transportData.getStation(endId);
        logger.info(format("Plan journey from %s to %s on %s %s at %s", latLong, finalStation, queryDate.getDay(),
                queryDate, queryTime));

        Stream<Journey> journeys = locToLocPlanner.quickestRouteForLocation(latLong, finalStation, queryTime, queryDate, arriveBy);
        JourneyPlanRepresentation plan = createPlan(queryDate, journeys);
        journeys.close();
        return plan;
    }

    private JourneyPlanRepresentation createJourneyPlanEndsWithWalk(String startId, LatLong latLong, TramServiceDate queryDate,
                                                                    TramTime queryTime, boolean arriveBy) {
        if (!transportData.hasStationId(startId)) {
            String msg = "Unable to find start station from id " + startId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }

        Station startStation = transportData.getStation(startId);

        logger.info(format("Plan journey from %s to %s on %s %s at %s", startStation, latLong, queryDate.getDay(),
                queryDate, queryTime));

        Stream<Journey> journeys = locToLocPlanner.quickestRouteForLocation(startId, latLong, queryTime, queryDate, arriveBy);
        JourneyPlanRepresentation plan = createPlan(queryDate, journeys);
        journeys.close();
        return plan;
    }

    private JourneyPlanRepresentation createJourneyPlan(String startId, String endId, TramServiceDate queryDate,
                                                        TramTime queryTime, boolean arriveBy) {
        if (!transportData.hasStationId(startId)) {
            String msg = "Unable to find start station from id " + startId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }

        if (!transportData.hasStationId(endId)) {
            String msg = "Unable to find end station from id " + endId;
            logger.warn(msg);
            throw new RuntimeException(msg);
        }

        Station startStation = transportData.getStation(startId);
        Station endStation = transportData.getStation(endId);

        logger.info(format("Plan journey from %s to %s on %s %s at %s (arrive by = %s)", startStation,
                endStation, queryDate.getDay(), queryDate,queryTime, arriveBy));

        Stream<Journey> journeys;
        if (arriveBy) {
            journeys = routeCalculatorArriveBy.calculateRoute(startId, endStation, queryTime, queryDate);
        } else {
            journeys = routeCalculator.calculateRoute(startId, endStation, queryTime, queryDate);
        }
        // ASSUME: Limit here rely's on search giving lowest cost routes first
        JourneyPlanRepresentation journeyPlanRepresentation = createPlan(queryDate, journeys.limit(config.getMaxNumResults()));
        journeys.close();
        return journeyPlanRepresentation;
    }

    private JourneyPlanRepresentation createPlan(TramServiceDate queryDate, Stream<Journey> journeys) {
        SortedSet<JourneyDTO> journeyDTOs = journeysMapper.createJourneyDTOs(journeys, queryDate, config.getMaxNumResults());
        List<String> notes = providesNotes.createNotesForJourneys(journeyDTOs, queryDate);
        return new JourneyPlanRepresentation(journeyDTOs, notes);
    }

}
