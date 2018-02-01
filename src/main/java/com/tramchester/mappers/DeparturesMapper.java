package com.tramchester.mappers;

import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;

import java.util.LinkedList;
import java.util.List;

public class DeparturesMapper {
    List<DepartureDTO> departs = new LinkedList<>();

    public List<DepartureDTO> fromStations(List<StationDTO> enrichedStations) {
        enrichedStations.forEach(stationDTO -> stationDTO.getPlatforms().forEach(platformDTO -> {
            StationDepartureInfo info = platformDTO.getStationDepartureInfo();
            if (info!=null) {
                info.getDueTrams().forEach(dueTram -> {
                    departs.add(new DepartureDTO(info.getLocation(),dueTram));
                });
            }
        }));

        return departs;
    }
}
