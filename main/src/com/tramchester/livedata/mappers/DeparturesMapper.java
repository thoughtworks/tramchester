package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.livedata.domain.DTO.DepartureDTO;

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
