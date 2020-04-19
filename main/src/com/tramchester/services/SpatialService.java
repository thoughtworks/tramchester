package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProximityGroup;
import com.tramchester.geo.StationLocations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SpatialService {
    private static final Logger logger = LoggerFactory.getLogger(SpatialService.class);

    private final TramchesterConfig config;
    private final StationLocations stationLocations;

    public SpatialService(TramchesterConfig config, StationLocations stationLocations) {
        this.config = config;
        this.stationLocations = stationLocations;
    }

    public List<Station> getNearestStations(LatLong latLong) {
        return getNearestStationsTo(latLong, config.getNumOfNearestStops(), config.getNearestStopRangeKM());
    }

    public List<StationDTO> reorderNearestStations(LatLong latLong, List<Station> sortedStations) {
        List<Station> seen = new LinkedList<>();

        List<Station> nearestStations = stationLocations.nearestStations(latLong, config.getNumOfNearestStops(),
                config.getNearestStopRangeKM());
        List<StationDTO> reorderedStations = new ArrayList<>();

        if (nearestStations.size()==0) {
            logger.warn("Unable to find stations close to " + latLong);
        }

        for (Station nearestStation : nearestStations) {
            StationDTO displayStation = new StationDTO(nearestStation, ProximityGroup.NEAREST_STOPS);
            reorderedStations.add(displayStation);
            seen.add(nearestStation);
        }

        List<StationDTO> remainingStations = sortedStations.stream().filter(station -> !seen.contains(station)).
                map(station -> new StationDTO(station, ProximityGroup.ALL)).
                collect(Collectors.toList());

        reorderedStations.addAll(remainingStations);
        return reorderedStations;

    }

    public List<Station> getNearestStationsTo(LatLong latLong, int numberOfNearest, double rangeInKM) {
        List<Station> result = stationLocations.nearestStations(latLong, numberOfNearest, rangeInKM);
        logger.info(format("Found %s stations close to %s", result.size(), latLong));
        return result;
    }

}
