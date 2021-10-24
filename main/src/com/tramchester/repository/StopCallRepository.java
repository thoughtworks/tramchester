package com.tramchester.repository;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class StopCallRepository {
    private static final Logger logger = LoggerFactory.getLogger(StopCallRepository.class);

    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final ServiceRepository serviceRepository;
    private final Map<Station, Set<StopCall>> stopCalls;

    @Inject
    public StopCallRepository(TripRepository tripRepository, StationRepository stationRepository, ServiceRepository serviceRepository) {
        this.tripRepository = tripRepository;
        this.stationRepository = stationRepository;
        this.serviceRepository = serviceRepository;
        stopCalls = new HashMap<>();
    }

    @PostConstruct
    public void start() {
        logger.info("starting");

        stationRepository.getStationStream().forEach(station -> stopCalls.put(station, new HashSet<>()));

        Set<Trip> allTrips = tripRepository.getTrips();
        allTrips.stream().
                flatMap(trip -> trip.getStopCalls().stream()).
                forEach(stopCall -> stopCalls.get(stopCall.getStation()).add(stopCall));

        long noStops = stopCalls.entrySet().stream().
                filter(entry -> entry.getValue().isEmpty()).
                count();

        logger.info("Added stopcalls for " + (stopCalls.size() - noStops) + " stations");
        if (noStops > 0) {
            logger.warn(noStops + " stations have no StopCalls");
        }
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        stopCalls.clear();
    }

    public Set<StopCall> getStopCallsFor(Station station, LocalDate date, TramTime begin, TramTime end) {
        Set<Service> runningOnDate = serviceRepository.getServicesOnDate(TramServiceDate.of(date));
        Set<StopCall> allForStation = stopCalls.get(station);

        return allForStation.stream().
                filter(stopCall -> stopCall.getPickupType().equals(GTFSPickupDropoffType.Regular)).
                filter(stopCall -> runningOnDate.contains(stopCall.getTrip().getService())).
                filter(stopCall -> stopCall.getArrivalTime().between(begin, end)).
                collect(Collectors.toSet());
    }
}