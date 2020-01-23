package com.tramchester.mappers;

import com.tramchester.domain.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.repository.LiveDataRepository;

import java.util.*;
import java.util.stream.Collectors;

public class DeparturesMapper {
    private static String DUE = "Due";

    private final ProvidesNotes providesNotes;
    private final LiveDataRepository liveDataRepository;

    public DeparturesMapper(ProvidesNotes providesNotes, LiveDataRepository liveDataRepository) {
        this.providesNotes = providesNotes;
        this.liveDataRepository = liveDataRepository;
    }

    @Deprecated
    public SortedSet<DepartureDTO> fromStations(List<StationDTO> enrichedStations) {
        SortedSet<DepartureDTO> departs = new TreeSet<>();

        enrichedStations.forEach(stationDTO -> stationDTO.getPlatforms().forEach(platformDTO -> {
            StationDepartureInfoDTO info = platformDTO.getStationDepartureInfo();
            if (info!=null) {
                info.getDueTrams().
                        stream().
                        filter(dueTram -> DUE.equals(dueTram.getStatus())).
                        forEach(dueTram -> departs.add(dueTram));
            }
        }));

        return departs;
    }

    public SortedSet<DepartureDTO> createDeparturesFor(List<Station> stations) {
        SortedSet<DepartureDTO> departs = new TreeSet<>();

        stations.forEach(station -> {
                    Set<DueTram> dueTrams = liveDataRepository.departuresFor(station).stream().
                            map(StationDepartureInfo::getDueTrams).flatMap(Collection::stream).
                            filter(dueTram -> DUE.equals(dueTram.getStatus())).
                            collect(Collectors.toSet());
                    dueTrams.stream().map(dueTram -> new DepartureDTO(station.getName(), dueTram)).
                            forEach(departs::add);
                });

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
