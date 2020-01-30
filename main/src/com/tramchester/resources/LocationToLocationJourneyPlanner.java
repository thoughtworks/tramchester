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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public Stream<Journey> quickestRouteForLocation(LatLong latLong, String destinationId, TramTime queryTime,
                                                    TramServiceDate queryDate) {

        logger.info(format("Finding shortest path for %s --> %s on %s at %s",
                latLong, destinationId, queryDate, queryTime));

        List<String> nearbyStations = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);

        logger.info(format("Found %s stations close to %s", nearbyStations.size(), latLong));
        return createJourneyPlan(latLong, nearbyStations, destinationId, queryTime, queryDate);
    }

    public Stream<Journey> quickestRouteForLocation(String startId, LatLong latLong, List<TramTime> queryTimes,
                                                    TramServiceDate queryDate) {
        List<String> nearbyStations = spatialService.getNearestStationsTo(latLong, Integer.MAX_VALUE);

        List<Location> starts = nearbyStations.stream().map(id -> stationRepository.getStation(id)).
                filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        List<StationWalk> walksToDest = starts.stream().map(station ->
                new StationWalk(station, findCostInMinutes(latLong, station))).collect(Collectors.toList());

        return routeCalculator.calculateRouteWalkAtEnd(startId, latLong, walksToDest, queryTimes, queryDate);
    }

    private Stream<Journey> createJourneyPlan(LatLong latLong, List<String> startIds, String destinationId, TramTime queryTime,
                                              TramServiceDate queryDate) {
        List<Location> starts = startIds.stream().map(id -> stationRepository.getStation(id)).
                filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());

        List<StationWalk> walksToStart = starts.stream().map(station ->
                new StationWalk(station, findCostInMinutes(latLong, station))).collect(Collectors.toList());

        return routeCalculator.calculateRoute(latLong, walksToStart, destinationId, queryTime, queryDate);
    }

    private int findCostInMinutes(LatLong latLong, Location station) {
        LatLng point1 = LatLong.getLatLng(latLong);
        LatLng point2 = LatLong.getLatLng(station.getLatLong());

        double distanceInMiles = LatLngTool.distance(point1, point2, LengthUnit.MILE);
        double hours = distanceInMiles / walkingSpeed;
        return (int)Math.ceil(hours * 60D);
    }
}
