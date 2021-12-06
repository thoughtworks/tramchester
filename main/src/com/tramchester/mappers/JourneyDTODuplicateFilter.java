package com.tramchester.mappers;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.StationRefWithPosition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@LazySingleton
public class JourneyDTODuplicateFilter {

    public Set<JourneyDTO> apply(Set<JourneyDTO> journeys) {
        Set<EqualityFacade> unique = journeys.stream().map(EqualityFacade::new).collect(Collectors.toSet());
        return unique.stream().map(EqualityFacade::getJourneyDTO).collect(Collectors.toSet());
    }

    private static class EqualityFacade {
        private final JourneyDTO journeyDTO;
        private final LocalDateTime departTime;
        private final LocalDateTime arrivalTime;
        private final List<StationRefWithPosition> path;

        private EqualityFacade(JourneyDTO journeyDTO) {
            this.journeyDTO = journeyDTO;
            this.departTime = journeyDTO.getFirstDepartureTime();
            this.arrivalTime = journeyDTO.getExpectedArrivalTime();
            this.path = journeyDTO.getPath();
        }

        public JourneyDTO getJourneyDTO() {
            return journeyDTO;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EqualityFacade that = (EqualityFacade) o;
            return departTime.equals(that.departTime) &&
                    arrivalTime.equals(that.arrivalTime) &&
                    path.equals(that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(departTime);
        }
    }
}
