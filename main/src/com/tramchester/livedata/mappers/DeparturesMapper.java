package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class DeparturesMapper {
    public static final String DUE = "Due";

    public DeparturesMapper() {
    }

    public Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> dueTrams, LocalDate queryDate) {
        return dueTrams.stream().
                    map(dueTram -> new DepartureDTO(dueTram.getDisplayLocation(), dueTram, queryDate))
                    .collect(Collectors.toSet());
    }
}
