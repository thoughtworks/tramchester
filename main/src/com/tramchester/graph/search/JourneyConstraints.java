package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.repository.RunningServices;
import com.tramchester.repository.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class JourneyConstraints {

    private static final int BUSES_MAX_PATH_LENGTH = 1000; // todo right value?
    private static final int TRAMS_MAX_PATH_LENGTH = 400;
    private static final int TRAINS_MAX_PATH_LENGTH = 2000; // todo right value?

    private static final Logger logger = LoggerFactory.getLogger(JourneyConstraints.class);

    private final RunningServices runningServices;
    private final TramchesterConfig config;
    private final int maxPathLength;
    private final Set<Station> endTramStations;
    private final boolean tramOnlyDestinations;
    private final int maxJourneyDuration;

    public JourneyConstraints(TramchesterConfig config, TransportData transportData, JourneyRequest journeyRequest,
                              Set<Station> endStations) {
        this.config = config;
        this.runningServices = new RunningServices(journeyRequest.getDate(), transportData);
        this.maxPathLength = computeMaxPathLength();

        endTramStations = endStations.stream().
                filter(TransportMode::isTram).
                collect(Collectors.toSet());

        tramOnlyDestinations = (endTramStations.size() == endStations.size());
        this.maxJourneyDuration = journeyRequest.getMaxJourneyDuration();

        if (tramOnlyDestinations) {
            logger.info("Checking only for tram destinations");
        }
    }

    private int computeMaxPathLength() {
        return config.getTransportModes().stream().map(this::getPathMaxFor).max(Integer::compareTo).get();
    }

    private int getPathMaxFor(GTFSTransportationType mode) {
        switch (mode) {
            case tram: return TRAMS_MAX_PATH_LENGTH;
            case bus: return BUSES_MAX_PATH_LENGTH;
            case train: return TRAINS_MAX_PATH_LENGTH;
            default:
                throw new RuntimeException("Unexpected transport mode " + mode);
        }
    }

    public boolean isRunning(IdFor<Service> serviceId) {
        return runningServices.isRunning(serviceId);
    }

    public ServiceTime getServiceEarliest(IdFor<Service> serviceId) {
        return runningServices.getServiceEarliest(serviceId);
    }

    public ServiceTime getServiceLatest(IdFor<Service> serviceId) {
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
}
