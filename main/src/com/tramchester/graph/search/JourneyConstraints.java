package com.tramchester.graph.search;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.repository.RunningServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class JourneyConstraints {
    private static final Logger logger = LoggerFactory.getLogger(JourneyConstraints.class);

    private final RunningServices runningServices;
    private final TramchesterConfig config;
    private final int maxPathLength;
    private final Set<Station> endTramStations;
    private final boolean tramOnlyDestinations;
    private final int maxJourneyDuration;

    public JourneyConstraints(TramchesterConfig config, RunningServices runningServices, int maxPathLength,
                              Set<Station> endStations, int maxJourneyDuration) {
        this.config = config;
        this.runningServices = runningServices;
        this.maxPathLength = maxPathLength;


        endTramStations = endStations.stream().
                filter(TransportMode::isTram).
                collect(Collectors.toSet());

        tramOnlyDestinations = (endTramStations.size() == endStations.size());
        this.maxJourneyDuration = maxJourneyDuration;

        if (tramOnlyDestinations) {
            logger.info("Checking only for tram destinations");
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
