package com.tramchester.livedata.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.livedata.domain.liveUpdates.UpcomingDeparture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class DeparturesMapper {
    public static final String DUE = "Due";

    public DeparturesMapper() {
    }

    public Set<DepartureDTO> mapToDTO(Collection<UpcomingDeparture> dueTrams, LocalDateTime lastUpdate) {
        return dueTrams.stream().
                    map(dueTram -> new DepartureDTO(dueTram.getDisplayLocation(), dueTram, lastUpdate))
                    .collect(Collectors.toSet());
    }
}
