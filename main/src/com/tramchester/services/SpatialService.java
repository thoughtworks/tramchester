package com.tramchester.services;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.places.ProximityGroups;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
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


    public List<StationDTO> reorderNearestStations(LatLong latLong, List<Station> sortedStations) {
        List<Station> seen = new LinkedList<>();

        List<Station> nearestStations = stationLocations.nearestStationsSorted(latLong, config.getNumOfNearestStops(),
                config.getNearestStopRangeKM());
        List<StationDTO> reorderedStations = new ArrayList<>();

        if (nearestStations.size()==0) {
            logger.warn("Unable to find stations close to " + latLong);
        }

        for (Station nearestStation : nearestStations) {
            StationDTO displayStation = new StationDTO(nearestStation, ProximityGroups.NEAREST_STOPS);
            reorderedStations.add(displayStation);
            seen.add(nearestStation);
        }

        List<StationDTO> remainingStations = sortedStations.stream().filter(station -> !seen.contains(station)).
                map(station -> new StationDTO(station, ProximityGroups.STOPS)).
                collect(Collectors.toList());

        reorderedStations.addAll(remainingStations);
        return reorderedStations;

    }

}
