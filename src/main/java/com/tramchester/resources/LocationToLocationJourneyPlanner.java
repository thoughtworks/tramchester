package com.tramchester.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LocationToLocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationToLocationJourneyPlanner.class);

    private final ObjectMapper objectMapper;
    private SpatialService spatialService;
    private TramchesterConfig config;
    private RouteCalculator routeCalculator;
    private StationRepository stationRepository;

    public LocationToLocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config, RouteCalculator routeCalculator, StationRepository stationRepository) {
        this.spatialService = spatialService;
        this.config = config;
        this.routeCalculator = routeCalculator;
        this.stationRepository = stationRepository;
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
        List<String> starts = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);

        return createJourneyPlan(latLong, starts,endId,minutesFromMidnight,dayOfWeek,queryDate);
    }

    private Set<RawJourney>  createJourneyPlan(LatLong latLong, List<String> startIds, String endId, int minutesFromMidnight, DaysOfWeek dayOfWeek,
                                               TramServiceDate queryDate) throws UnknownStationException {

        List<Location> starts = startIds.stream().map(id -> stationRepository.getStation(id)).collect(Collectors.toList());

        List<StationWalk> toStarts = starts.stream().map(station ->
                new StationWalk(station, findCost(latLong, station))).collect(Collectors.toList());

        Station end = stationRepository.getStation(endId);
        return routeCalculator.calculateRoute(latLong, toStarts, end, minutesFromMidnight, dayOfWeek, queryDate);
    }

    private int findCost(LatLong latLong, Location station) {
        LatLng point1 = new LatLng(latLong.getLat(), latLong.getLon());
        LatLng point2 = new LatLng(station.getLatitude(), station.getLongitude());
        double distanceInMiles = LatLngTool.distance(point1, point2, LengthUnit.MILE);

        double walkingSpeed = config.getWalkingMPH();

        double hours = distanceInMiles / walkingSpeed;

        return (int)Math.ceil(hours * 60D);
    }
}
