package com.tramchester.mappers;

import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.ProvidesNotes;

import java.util.*;

public class DeparturesMapper {
    private static String DUE = "Due";

    private final ProvidesNotes providesNotes;

    public DeparturesMapper(ProvidesNotes providesNotes) {
        this.providesNotes = providesNotes;
    }

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

    public DepartureListDTO from(List<StationDepartureInfo> departureInfos, boolean includeNotes) {
        SortedSet<DepartureDTO> trams = new TreeSet<>();

        List<String> notes = new ArrayList<>();
        if (includeNotes) {
            notes = providesNotes.createNotesFor(departureInfos);
        }

        departureInfos.forEach(info -> {
            String from = info.getLocation();

            info.getDueTrams().forEach(dueTram -> trams.add(new DepartureDTO(from,dueTram)));
        });

        return new DepartureListDTO(trams, notes);
    }
}
