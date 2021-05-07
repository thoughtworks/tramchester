package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@LazySingleton
public class DeparturesMapper {
    public static final String DUE = "Due";

    public DeparturesMapper() {
    }

    public Set<DepartureDTO> mapToDTO(Station station, Collection<DueTram> dueTrams, LocalDate queryDate) {
        return dueTrams.stream().
                    map(dueTram -> new DepartureDTO(station, dueTram, queryDate))
                    .collect(Collectors.toSet());
    }
}
