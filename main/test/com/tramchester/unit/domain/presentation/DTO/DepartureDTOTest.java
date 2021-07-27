package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.livedata.domain.liveUpdates.DueTram;
import com.tramchester.domain.places.Station;
import com.tramchester.livedata.domain.DTO.DepartureDTO;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.TreeSet;

import static com.tramchester.testSupport.reference.TramStations.StPetersSquare;

class DepartureDTOTest {

    private LocalTime updateTime;
    private LocalDate updateDate;

    @BeforeEach
    void beforeEachTestRuns() {
        LocalDateTime now = TestEnv.LocalNow();
        updateTime = now.toLocalTime();
        updateDate = now.toLocalDate();
    }

    @Test
    void shouldCreateFromDueTramAndLocation() {
        LocalTime updateTime = TestEnv.LocalNow().toLocalTime();
        if (updateTime.getHour()==23) {
            updateTime = updateTime.minusHours(1);
        }

        DueTram dueTram = getDueTram(updateTime, TramStations.Bury, 42);
        DepartureDTO departureDTO = new DepartureDTO(TramStations.of(StPetersSquare), dueTram, updateDate);

        Assertions.assertEquals(StPetersSquare.getName(), departureDTO.getFrom());
        Assertions.assertEquals("Bury", departureDTO.getDestination());
        Assertions.assertEquals("status", departureDTO.getStatus());
        Assertions.assertEquals("carriages", departureDTO.getCarriages());
        LocalDateTime when = departureDTO.getDueTime();
        Assertions.assertEquals(updateTime.plusMinutes(42).truncatedTo(ChronoUnit.MINUTES), when.toLocalTime());
        Assertions.assertEquals(updateDate, when.toLocalDate());
    }

    @Test
    void shouldCompareBasedOnWhenTramDue() {

        Station stPetersSquare = TramStations.of(StPetersSquare);
        DepartureDTO departureDTOA = new DepartureDTO(stPetersSquare,
                getDueTram(updateTime, TramStations.Deansgate, 5), updateDate);
        DepartureDTO departureDTOB = new DepartureDTO(stPetersSquare,
                getDueTram(updateTime, TramStations.Bury, 3), updateDate);
        DepartureDTO departureDTOC = new DepartureDTO(stPetersSquare,
                getDueTram(updateTime, TramStations.Piccadilly, 12), updateDate);

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
