package com.tramchester.resources;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.repository.StationRepository;
import com.tramchester.services.SpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class LocationToLocationJourneyPlanner {
    private static final Logger logger = LoggerFactory.getLogger(LocationToLocationJourneyPlanner.class);

    private final double walkingSpeed;
    private SpatialService spatialService;
    private RouteCalculator routeCalculator;
    private StationRepository stationRepository;

    public LocationToLocationJourneyPlanner(SpatialService spatialService, TramchesterConfig config,
                                            RouteCalculator routeCalculator, StationRepository stationRepository) {
        this.spatialService = spatialService;
        this.walkingSpeed = config.getWalkingMPH();
        this.routeCalculator = routeCalculator;
        this.stationRepository = stationRepository;
    }

    public Set<RawJourney> quickestRouteForLocation(LatLong latLong, String endId, List<LocalTime> queryTimes,
                                                    TramServiceDate queryDate) {

        List<String> starts = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);

        logger.info(format("Found %s stations close to %s", starts.size(), latLong));
        return createJourneyPlan(latLong, starts, endId, queryTimes, queryDate);
    }

    private Set<RawJourney> createJourneyPlan(LatLong latLong, List<String> startIds, String endId, List<LocalTime> queryTimes,
                                              TramServiceDate queryDate) {

        List<Location> starts = startIds.stream().map(id -> stationRepository.getStation(id).get()).collect(Collectors.toList());

        List<StationWalk> toStarts = starts.stream().map(station ->
                new StationWalk(station, findCostInMinutes(latLong, station))).collect(Collectors.toList());

        Station end = stationRepository.getStation(endId).get();
        return routeCalculator.calculateRoute(latLong, toStarts, end, queryTimes, queryDate, RouteCalculator.MAX_NUM_GRAPH_PATHS);
    }

    private int findCostInMinutes(LatLong latLong, Location station) {
        LatLng point1 = LatLong.getLatLng(latLong);
        LatLng point2 = LatLong.getLatLng(station.getLatLong());

        double distanceInMiles = LatLngTool.distance(point1, point2, LengthUnit.MILE);
        double hours = distanceInMiles / walkingSpeed;
        return (int)Math.ceil(hours * 60D);
    }
}
