package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.Route;
import com.tramchester.domain.RouteAndInterchanges;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.time.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Set;

@LazySingleton
public class StationAvailabilityRepository {
    private static final Logger logger = LoggerFactory.getLogger(StationAvailabilityRepository.class);

    private final StationRepository stationRepository;

    @Inject
    public StationAvailabilityRepository(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @PostConstruct
    @Inject
    public void start() {
        logger.info("Starting");


        logger.info("started");
    }

    public boolean isAvailable(Location<?> station, LocalDate when, TimeRange timeRange) {
        Set<Route> pickUps = station.getPickupRoutes(when, timeRange);
        Set<Route> dropOffs = station.getDropoffRoutes(when, timeRange);

        return ! ((pickUps.isEmpty() && dropOffs.isEmpty()));
    }

    public boolean isAvailable(RouteAndInterchanges routeAndInterchanges, LocalDate date, TimeRange time) {
        if (routeAndInterchanges.getRoutePair().isAvailableOn(date)) {
            return routeAndInterchanges.getInterchangeStations().stream().anyMatch(station -> isAvailable(station, date, time));
        }
        return false;
    }

    public boolean isAvailable(LocationSet locationSet, LocalDate date, TimeRange timeRange) {
        return locationSet.stream().anyMatch(location -> isAvailable(location, date, timeRange));
    }
}
