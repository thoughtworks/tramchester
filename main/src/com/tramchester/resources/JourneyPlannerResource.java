package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.livedata.LiveDataEnricher;
import com.tramchester.mappers.HeadsignMapper;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.repository.LiveDataRepository;
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
import java.time.LocalTime;
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
    private LiveDataRepository liveDataRepositoy;
    private GraphDatabaseService graphDatabaseService;

    public JourneyPlannerResource(RouteCalculator routeCalculator, JourneysMapper journeysMapper, TramchesterConfig config,
                                  LocationToLocationJourneyPlanner locToLocPlanner, CreateQueryTimes createQueryTimes,
                                  UpdateRecentJourneys updateRecentJourneys, ObjectMapper objectMapper,
                                  ProvidesNotes providesNotes, LiveDataRepository liveDataRepositoy,
                                  GraphDatabaseService graphDatabaseService) {
        super(updateRecentJourneys, objectMapper);
        this.routeCalculator = routeCalculator;
        this.journeysMapper = journeysMapper;
        this.config = config;
        this.locToLocPlanner = locToLocPlanner;
        this.createQueryTimes = createQueryTimes;
        this.providesNotes = providesNotes;
        this.liveDataRepositoy = liveDataRepositoy;
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
                    if (MyLocationFactory.MY_LOCATION_PLACEHOLDER_ID.equals(startId)) {
                        LatLong latLong = decodeLatLong(lat, lon);
                        planRepresentation = createJourneyPlan(latLong, endId, queryDate, departureTime);
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

    private LatLong decodeLatLong(String lat, String lon) {
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lon);
        return new LatLong(latitude,longitude);
    }

    public JourneyPlanRepresentation createJourneyPlan(LatLong latLong, String endId, TramServiceDate queryDate,
                                                       TramTime initialQueryTime) {
        logger.info(format("Plan journey from %s to %s on %s %s at %s", latLong, endId,queryDate.getDay(),
                queryDate, initialQueryTime));

        List<TramTime> queryTimes = Collections.singletonList(initialQueryTime);
        Stream<RawJourney> journeys = locToLocPlanner.quickestRouteForLocation(latLong, endId, queryTimes, queryDate);
        Set<RawJourney> rawJourneySet = journeys.collect(Collectors.toSet());

        return createPlan(queryDate, initialQueryTime, rawJourneySet);
    }

    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId, TramServiceDate queryDate,
                                                        TramTime initialQueryTime) {
        List<TramTime> queryTimes = createQueryTimes.generate(initialQueryTime);

        logger.info(format("Plan journey from %s to %s on %s %s at %s", startId, endId,queryDate.getDay(),
                queryDate,queryTimes));

        Stream<RawJourney> journeys = routeCalculator.calculateRoute(startId, endId, queryTimes, queryDate);
        Set<RawJourney> rawJourneySet = journeys.limit(config.getMaxNumResults()).collect(Collectors.toSet());

        return createPlan(queryDate, initialQueryTime, rawJourneySet);
    }

    private JourneyPlanRepresentation createPlan(TramServiceDate queryDate, TramTime initialQueryTime,
                                                 Set<RawJourney> journeys) {
        logger.info("number of journeys: " + journeys.size());
        JourneyDTOFactory factory = createJourneyDTOFactory(queryDate, initialQueryTime);
        SortedSet<JourneyDTO> decoratedJourneys = journeysMapper.map(factory, journeys);
        List<String> notes = providesNotes.createNotesForJourneys(queryDate, decoratedJourneys);
        return new JourneyPlanRepresentation(decoratedJourneys, notes);
    }

    private JourneyDTOFactory createJourneyDTOFactory(TramServiceDate queryDate, TramTime initialQueryTime) {
        // as query time and contents of live data changes need to create new factory each time
        LiveDataEnricher liveDataEnricher = new LiveDataEnricher(liveDataRepositoy, queryDate, initialQueryTime);
        StageDTOFactory stageDTOFactory = new StageDTOFactory(liveDataEnricher);
        HeadsignMapper headsignMapper = new HeadsignMapper();
        return new JourneyDTOFactory(stageDTOFactory, headsignMapper);
    }

}
