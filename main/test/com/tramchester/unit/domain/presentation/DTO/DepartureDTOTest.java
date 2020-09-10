package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

import static com.tramchester.testSupport.TramStations.StPetersSquare;

class DepartureDTOTest {

    @Test
    void shouldCreateFromDueTramAndLocation() {
        LocalTime updateTime = TestEnv.LocalNow().toLocalTime();

        DueTram dueTram = getDueTram(updateTime, TramStations.Bury, 42);
        DepartureDTO departureDTO = new DepartureDTO(TramStations.of(StPetersSquare), dueTram);

        Assertions.assertEquals(StPetersSquare.getName(), departureDTO.getFrom());
        Assertions.assertEquals("Bury", departureDTO.getDestination());
        Assertions.assertEquals("status", departureDTO.getStatus());
        Assertions.assertEquals("carriages", departureDTO.getCarriages());
        Assertions.assertEquals(TramTime.of(updateTime.plusMinutes(42)), departureDTO.getWhen());
    }

    @Test
    void shouldCompareBasedOnWhenTramDue() {
        LocalTime updateTime = TestEnv.LocalNow().toLocalTime();

        Station stPetersSquare = TramStations.of(StPetersSquare);
        DepartureDTO departureDTOA = new DepartureDTO(stPetersSquare, getDueTram(updateTime, TramStations.Deansgate, 5));
        DepartureDTO departureDTOB = new DepartureDTO(stPetersSquare, getDueTram(updateTime, TramStations.Bury, 3));
        DepartureDTO departureDTOC = new DepartureDTO(stPetersSquare, getDueTram(updateTime, TramStations.Piccadilly, 12));

        Set<DepartureDTO> list = new TreeSet<>();
        list.add(departureDTOA);
        list.add(departureDTOB);
        list.add(departureDTOC);

        DepartureDTO[] elements = list.toArray(new DepartureDTO[3]);
        Assertions.assertEquals("Bury", elements[0].getDestination());
        Assertions.assertEquals("Deansgate-Castlefield", elements[1].getDestination());
        Assertions.assertEquals("Piccadilly", elements[2].getDestination());
    }

    @NotNull
    private DueTram getDueTram(LocalTime updateTime, TramStations station, int wait) {
        return new DueTram(TramStations.of(station), "status", wait, "carriages", updateTime);
    }
}
