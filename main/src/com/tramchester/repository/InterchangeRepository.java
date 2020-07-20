package com.tramchester.repository;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.GTFSTransportationType;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.input.TramInterchanges;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class InterchangeRepository implements Disposable, Startable {
    private static final Logger logger = LoggerFactory.getLogger(InterchangeRepository.class);

    private final TransportDataSource dataSource;
    private final List<GTFSTransportationType> modes;

    // id -> Station
    private Map<String, Station> busInterchanges;
    // id -> Station
    private Map<String, Station> trainInterchanges;

    public InterchangeRepository(TransportDataSource dataSource, TramchesterConfig config) {
        this.dataSource = dataSource;
        // both of these empty for trams
        busInterchanges = Collections.emptyMap();
        trainInterchanges = Collections.emptyMap();
        modes = config.getTransportModes();
    }

    @Override
    public void dispose() {
        trainInterchanges.clear();
        busInterchanges.clear();
    }

    @Override
    public void start() {
        if (modes.contains(GTFSTransportationType.bus)) {
            busInterchanges = createBusInterchangeList();
            logger.info(format("Added %s bus interchanges", busInterchanges.size()));
        }
        if (modes.contains(GTFSTransportationType.train)) {
            trainInterchanges = createTrainMultiAgencyStationList();
            logger.info(format("Added %s train interchanges", trainInterchanges.size()));
        }
    }

    private Map<String, Station> createTrainMultiAgencyStationList() {
        return dataSource.getStations().stream().
            filter(TransportMode::isTrain).
            filter(station -> station.getAgencies().size()>=2).
            collect(Collectors.toMap(Station::getId, (station -> station)));
    }

    @Override
    public void stop() {
        // no op
    }

    private Map<String, Station> createBusInterchangeList() {
        logger.info("Finding bus interchanges based on names");
        return dataSource.getStations().stream().
                filter(TransportMode::isBus).
                filter(station -> checkForBusInterchange(station.getName())).
                collect(Collectors.toMap(Station::getId, (station -> station)));
    }

    // TODO WIP
    // Very crude - need better way
    private boolean checkForBusInterchange(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("interchange")) {
            return true;
        }
        return lower.contains("bus station") && (!lower.contains("adj bus station"));
    }

    public Collection<Station> getBusInterchanges() {
        return busInterchanges.values();
    }

    public boolean isInterchange(Station station) {
        if (TransportMode.isTram(station)) {
            return TramInterchanges.has(station);
        }
        if (TransportMode.isBus(station)) {
            return busInterchanges.containsValue(station);
        }
        if (TransportMode.isTrain(station)) {
            return trainInterchanges.containsValue(station);
        }
        logger.warn("Interchanges not defined for station of type " +station.getTransportMode() + " id was " + station.getId());
        return false;
    }

    public boolean isInterchange(String stationId) {
        if (TramInterchanges.has(stationId)) {
            return true;
        }
        if (busInterchanges.containsKey(stationId)) {
            return true;
        }
        return trainInterchanges.containsKey(stationId);
    }

}
