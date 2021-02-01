package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

public class JourneyConstraints {

    private static final int BUSES_MAX_PATH_LENGTH = 1000; // todo right value?
    private static final int TRAMS_MAX_PATH_LENGTH = 400;
    private static final int TRAINS_MAX_PATH_LENGTH = 2000; // todo right value?
    private static final int FERRY_MAX_PATH_LENGTH = 200; // todo right value?
    private static final int SUBWAY_MAX_PATH_LENGTH = 400; // todo right value?


    private static final Logger logger = LoggerFactory.getLogger(JourneyConstraints.class);

    private final RunningServices runningServices;
    private final TramchesterConfig config;
    private final int maxPathLength;
    private final Set<Station> endTramStations;
    private final IdSet<Station> closedStations;
    private final boolean tramOnlyDestinations;
    private final int maxJourneyDuration;

    public JourneyConstraints(TramchesterConfig config, TransportData transportData, JourneyRequest journeyRequest,
                              Set<Station> endStations) {
        this.config = config;
        this.runningServices = new RunningServices(journeyRequest.getDate(), transportData, config);
        this.maxPathLength = computeMaxPathLength();

        endTramStations = endStations.stream().
                filter(TransportMode::isTram).
                collect(Collectors.toSet());

        tramOnlyDestinations = (endTramStations.size() == endStations.size());
        this.maxJourneyDuration = journeyRequest.getMaxJourneyDuration();

        LocalDate date = journeyRequest.getDate().getDate();

        this.closedStations = config.getStationClosures().stream().
                filter(closure -> date.isAfter(closure.getBegin()) || date.isEqual(closure.getBegin()) ).
                filter(closure -> date.isBefore(closure.getEnd()) || date.isEqual(closure.getEnd())).
                map(StationClosure::getStation).
                collect(IdSet.idCollector());

        if (!closedStations.isEmpty()) {
            logger.info("Have closed stationed " + closedStations);
        }

        if (tramOnlyDestinations) {
            logger.info("Checking only for tram destinations");
        }
    }

    private int computeMaxPathLength() {
        return config.getTransportModes().stream().map(this::getPathMaxFor).max(Integer::compareTo).get();
    }

    private int getPathMaxFor(TransportMode mode) {
        switch (mode) {
            case Tram: return TRAMS_MAX_PATH_LENGTH;
            case RailReplacementBus:
            case Bus:
                return BUSES_MAX_PATH_LENGTH;
            case Train: return TRAINS_MAX_PATH_LENGTH;
            case Subway: return SUBWAY_MAX_PATH_LENGTH;
            case Ferry: return FERRY_MAX_PATH_LENGTH;
            default:
                throw new RuntimeException("Unexpected transport mode " + mode);
        }
    }

    public boolean isRunning(StringIdFor<Service> serviceId) {
        return runningServices.isRunning(serviceId);
    }

    public TramTime getServiceEarliest(StringIdFor<Service> serviceId) {
        return runningServices.getServiceEarliest(serviceId);
    }

    public TramTime getServiceLatest(StringIdFor<Service> serviceId) {
        return runningServices.getServiceLatest(serviceId);
    }

    public int getMaxWait() {
        return config.getMaxWait();
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public Set<Station> getEndTramStations() {
        return endTramStations;
    }

    public boolean getIsTramOnlyDestinations() {
        return tramOnlyDestinations;
    }

    public int getMaxJourneyDuration() {
        return maxJourneyDuration;
    }

    public boolean isClosed(Station station) {
        return closedStations.contains(station.getId());
    }
}
