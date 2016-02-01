package com.tramchester.resources;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TimeWindow;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.mappers.GenericJourneyResponseMapper;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.mappers.TramJourneyResponseMapper;
import com.tramchester.services.DateTimeService;
import org.joda.time.LocalDate;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;

@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private final TramchesterConfig config;
    private RouteCalculator routeCalculator;
    private DateTimeService dateTimeService;
    private JourneyResponseMapper journeyResponseMapper;
    //private int timeWindow = 60; // TODO into config

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService,
                                  JourneyResponseMapper journeyResponseMapper, TramchesterConfig config) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
        this.journeyResponseMapper = journeyResponseMapper;
        this.config = config;
    }

    @GET
    public Response quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTime){
        DaysOfWeek dayOfWeek = DaysOfWeek.fromToday();
        // today expose this as a parameter
        TramServiceDate queryDate = new TramServiceDate(LocalDate.now());
        try {
            JourneyPlanRepresentation  planRepresentation = createJourneyPlan(startId, endId, departureTime, dayOfWeek, queryDate);
            return Response.ok(planRepresentation).build();
        } catch (TramchesterException exception) {
            logger.error("Unable to plan journey",exception);
        }
        return Response.serverError().build();
    }

    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId, String queryTime,
                                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws TramchesterException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(queryTime);
        logger.info(String.format("start: %s end: %s query time: %s (%s) on %s",
                startId, endId, queryTime, minutesFromMidnight, dayOfWeek));

        Set<RawJourney> journeys = routeCalculator.calculateRoute(startId, endId, minutesFromMidnight, dayOfWeek, queryDate);
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, new TimeWindow(minutesFromMidnight, config.getTimeWindow()));
    }

    public JourneyPlanRepresentation createJourneyPlan(List<Node> starts, List<Node> ends, String queryTime,
                                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws TramchesterException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(queryTime);

        Set<RawJourney> journeys = routeCalculator.calculateRoute(starts, ends, minutesFromMidnight, dayOfWeek, queryDate);
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, new TimeWindow(minutesFromMidnight, config.getTimeWindow()));
    }
}
