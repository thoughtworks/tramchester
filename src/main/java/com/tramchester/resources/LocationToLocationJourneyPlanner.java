package com.tramchester.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.services.DateTimeService;
import com.tramchester.services.SpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class LocationToLocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationToLocationJourneyPlanner.class);

    private final ObjectMapper objectMapper;
    private SpatialService spatialService;
    private TramchesterConfig config;
    private RouteCalculator routeCalculator;

    public LocationToLocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config, RouteCalculator routeCalculator) {
        this.spatialService = spatialService;
        this.config = config;
        this.routeCalculator = routeCalculator;
        objectMapper = new ObjectMapper();
    }

    public Set<RawJourney> quickestRouteForLocation(String startId, String endId, int minutesFromMidnight,
                                                    DaysOfWeek dayOfWeek, TramServiceDate queryDate) throws TramchesterException {
        LatLong latLong;
        try {
            latLong = objectMapper.readValue(startId, LatLong.class);
        } catch (IOException e) {
            String msg = "Unable to process lat/long" + startId;
            logger.error(msg,e);
            throw new TramchesterException(msg, e);
        }
        List<String> starts = spatialService.getNearestStationsTo(latLong, config.getNumOfNearestStops());
        List<String> ends = Arrays.asList(endId);

        return createJourneyPlan(starts,ends,minutesFromMidnight,dayOfWeek,queryDate);
    }

    private Set<RawJourney>  createJourneyPlan(List<String> starts, List<String> ends, int minutesFromMidnight, DaysOfWeek dayOfWeek,
                                               TramServiceDate queryDate) throws UnknownStationException {

        return routeCalculator.calculateRoute(starts, ends, minutesFromMidnight, dayOfWeek, queryDate);
    }
}
