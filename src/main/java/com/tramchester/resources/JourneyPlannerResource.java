package com.tramchester.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TimeWindow;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.mappers.JourneyResponseMapper;
import com.tramchester.services.DateTimeService;
import com.tramchester.services.SpatialService;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Path("/journey")
@Produces(MediaType.APPLICATION_JSON)
public class JourneyPlannerResource {
    private static final Logger logger = LoggerFactory.getLogger(JourneyPlannerResource.class);
    private final TramchesterConfig config;
    private SpatialService spatialService;
    private final ObjectMapper objectMapper;
    private RouteCalculator routeCalculator;
    private DateTimeService dateTimeService;
    private JourneyResponseMapper journeyResponseMapper;

    public JourneyPlannerResource(RouteCalculator routeCalculator, DateTimeService dateTimeService,
                                  JourneyResponseMapper journeyResponseMapper, TramchesterConfig config,
                                  SpatialService spatialService) {
        this.routeCalculator = routeCalculator;
        this.dateTimeService = dateTimeService;
        this.journeyResponseMapper = journeyResponseMapper;
        this.config = config;
        this.spatialService = spatialService;
        objectMapper = new ObjectMapper();
    }

    @GET
    public Response quickestRoute(@QueryParam("start") String startId, @QueryParam("end") String endId,
                                  @QueryParam("departureTime") String departureTime){
        DaysOfWeek dayOfWeek = DaysOfWeek.fromToday();
        // TODO expose this as a parameter
        TramServiceDate queryDate = new TramServiceDate(LocalDate.now());

        JourneyPlanRepresentation planRepresentation;
        try {
            if (startId.startsWith("{") && startId.endsWith("}")) {
                planRepresentation =  quickestRouteForLocation(startId, endId, departureTime, dayOfWeek, queryDate);
            } else {
                planRepresentation = createJourneyPlan(startId, endId, departureTime, dayOfWeek, queryDate);
            }
            return Response.ok(planRepresentation).build();
        } catch (TramchesterException exception) {
            logger.error("Unable to plan journey",exception);
        } catch (IOException exception) {
            logger.error("Unable to plan journey",exception);
        }
        return Response.serverError().build();
    }

    private JourneyPlanRepresentation quickestRouteForLocation(String startId, String endId, String departureTime,
                                                               DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws IOException, TramchesterException {
        LatLong latLong = objectMapper.readValue(startId, LatLong.class);
        List<String> starts = spatialService.getNearestStationsTo(latLong, config.getNumOfNearestStops());
        List<String> ends = Arrays.asList(endId);
        return createJourneyPlan(starts,ends,departureTime,dayOfWeek,queryDate);
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

    public JourneyPlanRepresentation createJourneyPlan(List<String> starts, List<String> ends, String queryTime,
                                                       DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws TramchesterException {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight(queryTime);

        Set<RawJourney> journeys = routeCalculator.calculateRoute(starts, ends, minutesFromMidnight, dayOfWeek, queryDate);
        logger.info("number of journeys: " + journeys.size());
        return journeyResponseMapper.map(journeys, new TimeWindow(minutesFromMidnight, config.getTimeWindow()));
    }
}
