package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TimeWindow;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.services.DateTimeService;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

import static java.lang.String.format;

@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private final TramchesterConfig config;
    private LocationToLocationJourneyPlanner locToLocPlanner;
    private RouteCalculator routeCalculator;
    private DateTimeService dateTimeService;
    private JourneyResponseMapper journeyResponseMapper;

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService,
                                  JourneyResponseMapper journeyResponseMapper, TramchesterConfig config,
                                  LocationToLocationJourneyPlanner locToLocPlanner) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
        this.journeyResponseMapper = journeyResponseMapper;
        this.config = config;
        this.locToLocPlanner = locToLocPlanner;
    }

    @GET
    @Timed
    public Response quickestRoute(@QueryParam("start") String startId,
                                  @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTime,
                                  @QueryParam("departureDate") String departureDate){
        logger.info(format("Plan journey from %s to %s at %s on %s", startId, endId,departureTime, departureDate));

        LocalDate date = new LocalDate(departureDate);
        TramServiceDate queryDate = new TramServiceDate(date);

        try {
            int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(departureTime);
            JourneyPlanRepresentation planRepresentation = createJourneyPlan(startId, endId, queryDate, minutesFromMidnight);
            Response response = Response.ok(planRepresentation).build();
            return response;
        } catch (TramchesterException exception) {
            logger.error("Unable to plan journey",exception);
        } catch(Exception exception) {
            logger.error("Problem processing response", exception);
        }

        return Response.serverError().build();
    }

    public JourneyPlanRepresentation createJourneyPlan(String startId, String endId,
                                                       TramServiceDate queryDate, int minutesFromMidnight)
            throws TramchesterException {
        logger.info(format("Plan journey from %s to %s on %s %s at %s", startId, endId,queryDate.getDay(),queryDate,minutesFromMidnight));
        Set<RawJourney> journeys;
        if (startId.startsWith("{") && startId.endsWith("}")) {
            journeys = locToLocPlanner.quickestRouteForLocation(startId, endId, minutesFromMidnight, queryDate);
        } else {
           journeys = routeCalculator.calculateRoute(startId, endId, minutesFromMidnight, queryDate);
        }
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, new TimeWindow(minutesFromMidnight, config.getTimeWindow()));
    }

}
