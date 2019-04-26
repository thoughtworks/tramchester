package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.livedata.LiveDataEnricher;
import com.tramchester.mappers.HeadsignMapper;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.repository.LiveDataRepository;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

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

    public JourneyPlannerResource(RouteCalculator routeCalculator,
                                  JourneysMapper journeysMapper, TramchesterConfig config,
                                  LocationToLocationJourneyPlanner locToLocPlanner, CreateQueryTimes createQueryTimes,
                                  UpdateRecentJourneys updateRecentJourneys, ObjectMapper objectMapper, ProvidesNotes providesNotes, LiveDataRepository liveDataRepositoy) {
        super(updateRecentJourneys, objectMapper);
        this.routeCalculator = routeCalculator;
        this.journeysMapper = journeysMapper;
        this.config = config;
        this.locToLocPlanner = locToLocPlanner;
        this.createQueryTimes = createQueryTimes;
        this.providesNotes = providesNotes;
        this.liveDataRepositoy = liveDataRepositoy;
    }

    @GET
    @Timed
    @ApiOperation(value = "Find quickest route", response = JourneyPlanRepresentation.class)
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.HOURS)
    public Response quickestRoute(@QueryParam("start") String startId,
                                  @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTimeText,
                                  @QueryParam("departureDate") String departureDate,
                                  @CookieParam(StationResource.TRAMCHESTER_RECENT) Cookie cookie){
        logger.info(format("Plan journey from %s to %s at %s on %s", startId, endId,departureTimeText, departureDate));

        LocalDate date = LocalDate.parse(departureDate); // TODO need formatter?
        TramServiceDate queryDate = new TramServiceDate(date);

        try {
            Optional<TramTime> maybeDepartureTime = TramTime.parse(departureTimeText);
            if (maybeDepartureTime.isPresent()) {
                TramTime departureTime = maybeDepartureTime.get();
                JourneyPlanRepresentation planRepresentation = createJourneyPlan(startId, endId, queryDate,
                    departureTime);

                Response.ResponseBuilder responseBuilder = Response.ok(planRepresentation);
                responseBuilder.cookie(createRecentCookie(cookie, startId, endId));
                Response response = responseBuilder.build();
                return response;
            }

        } catch (TramchesterException exception) {
            logger.error("Unable to plan journey",exception);
        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
        }

        return Response.serverError().build();
    }

    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId,
                                                       TramServiceDate queryDate, TramTime initialQueryTime)
            throws TramchesterException {
        logger.info(format("Plan journey from %s to %s on %s %s at %s", startId, endId,queryDate.getDay(),
                queryDate,initialQueryTime));

        Set<RawJourney> journeys;
        List<LocalTime> queryTimes = createQueryTimes.generate(initialQueryTime.asLocalTime());
        if (isFromMyLocation(startId)) {
            journeys = locToLocPlanner.quickestRouteForLocation(startId, endId, queryTimes, queryDate);
        } else {
            journeys = routeCalculator.calculateRoute(startId, endId, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
        }
        logger.info("number of journeys: " + journeys.size());
        JourneyDTOFactory factory = createJourneyDTOFactory(queryDate, initialQueryTime);
        SortedSet<JourneyDTO> decoratedJourneys = journeysMapper.map(factory, journeys, config.getTimeWindow());
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
