package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

import static junit.framework.TestCase.assertEquals;

class DepartureDTOTest {

    @Test
    void shouldCreateFromDueTramAndLocation() {
        LocalTime updateTime = TestEnv.LocalNow().toLocalTime();

        Station location = Stations.StPetersSquare;
        DueTram dueTram = new DueTram(Stations.Bury, "status", 42, "carriages", updateTime);
        DepartureDTO departureDTO = new DepartureDTO(location, dueTram);

        Assertions.assertEquals(Stations.StPetersSquare.getName(), departureDTO.getFrom());
        Assertions.assertEquals("Bury", departureDTO.getDestination());
        Assertions.assertEquals("status", departureDTO.getStatus());
        Assertions.assertEquals("carriages", departureDTO.getCarriages());
        Assertions.assertEquals(TramTime.of(updateTime.plusMinutes(42)), departureDTO.getWhen());
    }

    @Test
    void shouldCompareBasedOnWhenTramDue() {
        LocalTime updateTime = TestEnv.LocalNow().toLocalTime();

        DepartureDTO departureDTOA = new DepartureDTO(Stations.StPetersSquare,
                new DueTram(Stations.Deansgate, "status", 5, "carriages", updateTime));
        DepartureDTO departureDTOB = new DepartureDTO(Stations.StPetersSquare,
                new DueTram(Stations.Bury, "status", 3, "carriages", updateTime));
        DepartureDTO departureDTOC = new DepartureDTO(Stations.StPetersSquare,
                new DueTram(Stations.Piccadilly, "status", 12, "carriages", updateTime));

        Set<DepartureDTO> list = new TreeSet<>();
        list.add(departureDTOA);
        list.add(departureDTOB);
        list.add(departureDTOC);

        DepartureDTO[] elements = list.toArray(new DepartureDTO[3]);
        Assertions.assertEquals("Bury", elements[0].getDestination());
        Assertions.assertEquals("Deansgate-Castlefield", elements[1].getDestination());
        Assertions.assertEquals("Piccadilly", elements[2].getDestination());

    }
}
