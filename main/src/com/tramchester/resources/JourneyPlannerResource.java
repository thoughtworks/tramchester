package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.repository.LiveDataSource;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Api
@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource extends UsesRecentCookie {

    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private final TramchesterConfig config;
    private LocationToLocationJourneyPlanner locToLocPlanner;
    private RouteCalculator routeCalculator;
    private JourneysMapper journeysMapper;
    private CreateQueryTimes createQueryTimes;
    private ProvidesNotes providesNotes;
    private GraphDatabaseService graphDatabaseService;

    public JourneyPlannerResource(RouteCalculator routeCalculator, JourneysMapper journeysMapper, TramchesterConfig config,
                                  LocationToLocationJourneyPlanner locToLocPlanner, CreateQueryTimes createQueryTimes,
                                  UpdateRecentJourneys updateRecentJourneys, ObjectMapper objectMapper,
                                  ProvidesNotes providesNotes, GraphDatabaseService graphDatabaseService) {
        super(updateRecentJourneys, objectMapper);
        this.routeCalculator = routeCalculator;
        this.journeysMapper = journeysMapper;
        this.config = config;
        this.locToLocPlanner = locToLocPlanner;
        this.createQueryTimes = createQueryTimes;
        this.providesNotes = providesNotes;
        this.graphDatabaseService = graphDatabaseService;
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
                                  @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie){
        logger.info(format("Plan journey from %s to %s at %s on %s", startId, endId,departureTimeText, departureDate));

        LocalDate date = LocalDate.parse(departureDate); // TODO need formatter?
        TramServiceDate queryDate = new TramServiceDate(date);

        try {
            Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeText);
            if (maybeDepartureTime.isPresent()) {
                TramTime departureTime = maybeDepartureTime.get();

                JourneyPlanRepresentation planRepresentation;
                try (Transaction tx = graphDatabaseService.beginTx() ) {
                    if (isWalking(startId)) {
                        LatLong latLong = decodeLatLong(lat, lon);
                        planRepresentation = createJourneyPlanStartsWithWalk(latLong, endId, queryDate, departureTime);
                    } else if (isWalking(endId)) {
                        LatLong latLong = decodeLatLong(lat, lon);
                        planRepresentation = createJourneyPlanEndsWithWalk(startId, latLong, queryDate, departureTime);
                    } else {
                        planRepresentation = createJourneyPlan(startId, endId, queryDate, departureTime);
                    }
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
                                                                      TramTime initialQueryTime) {
        logger.info(format("Plan journey from %s to %s on %s %s at %s", latLong, endId,queryDate.getDay(),
                queryDate, initialQueryTime));

        Stream<Journey> journeys = locToLocPlanner.quickestRouteForLocation(latLong, endId, initialQueryTime, queryDate);
        Set<Journey> journeySet = journeys.collect(Collectors.toSet());

        return createPlan(queryDate, journeySet);
    }

    private JourneyPlanRepresentation createJourneyPlanEndsWithWalk(String startId, LatLong latLong, TramServiceDate queryDate,
                                                                      TramTime initialQueryTime) {
        logger.info(format("Plan journey from %s to %s on %s %s at %s", startId, latLong, queryDate.getDay(),
                queryDate, initialQueryTime));

        Stream<Journey> journeys = locToLocPlanner.quickestRouteForLocation(startId, latLong, initialQueryTime, queryDate);
        Set<Journey> journeySet = journeys.collect(Collectors.toSet());

        return createPlan(queryDate, journeySet);
    }



    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId, TramServiceDate queryDate,
                                                        TramTime queryTime) {
        List<TramTime> queryTimes = createQueryTimes.generate(queryTime);

        logger.info(format("Plan journey from %s to %s on %s %s at %s", startId, endId,queryDate.getDay(),
                queryDate,queryTimes));

        Stream<Journey> journeys = routeCalculator.calculateRoute(startId, endId, queryTimes, queryDate);
        Set<Journey> journeySet = journeys.limit(config.getMaxNumResults()).collect(Collectors.toSet());

        return createPlan(queryDate, journeySet);
    }

    private JourneyPlanRepresentation createPlan(TramServiceDate queryDate, Set<Journey> journeys) {
        logger.info("number of journeys: " + journeys.size());

        List<String> notes = providesNotes.createNotesForJourneys(queryDate, journeys);
        SortedSet<JourneyDTO> journeyDTOs = journeysMapper.createJourneyDTOs(journeys, queryDate);
        return new JourneyPlanRepresentation(journeyDTOs, notes);
    }


}
