package com.tramchester.mappers;

import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class DeparturesMapper {
    private static String DUE = "Due";

    public SortedSet<DepartureDTO> fromStations(List<StationDTO> enrichedStations) {
        SortedSet<DepartureDTO> departs = new TreeSet<>();

        enrichedStations.forEach(stationDTO -> stationDTO.getPlatforms().forEach(platformDTO -> {
            StationDepartureInfo info = platformDTO.getStationDepartureInfo();
            if (info!=null) {
                info.getDueTrams().
                        stream().
                        filter(dueTram -> DUE.equals(dueTram.getStatus())).
                        forEach(dueTram -> departs.add(new DepartureDTO(info.getLocation(),dueTram)));
            }
        }));

        return departs;
    }
}
