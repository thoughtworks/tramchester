package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestConfig;
import org.junit.Test;

import java.time.LocalTime;
import java.util.Set;
import java.util.TreeSet;

import static junit.framework.TestCase.assertEquals;

public class DepartureDTOTest {

    @Test
    public void shouldCreateFromDueTramAndLocation() {
        LocalTime updateTime = TestConfig.LocalNow().toLocalTime();

        String location = "aPlace";
        DueTram dueTram = new DueTram(Stations.Bury, "status", 42, "carriages", updateTime);
        DepartureDTO departureDTO = new DepartureDTO(location, dueTram);

        assertEquals("aPlace", departureDTO.getFrom());
        assertEquals("Bury", departureDTO.getDestination());
        assertEquals("status", departureDTO.getStatus());
        assertEquals("carriages", departureDTO.getCarriages());
        assertEquals(TramTime.of(updateTime.plusMinutes(42)), departureDTO.getWhen());
    }

    @Test
    public void shouldCompareBasedOnWhenTramDue() {
        LocalTime updateTime = TestConfig.LocalNow().toLocalTime();

        DepartureDTO departureDTOA = new DepartureDTO("station", new DueTram(Stations.Deansgate, "status", 5, "carriages", updateTime));
        DepartureDTO departureDTOB = new DepartureDTO("station", new DueTram(Stations.Bury, "status", 3, "carriages", updateTime));
        DepartureDTO departureDTOC = new DepartureDTO("station", new DueTram(Stations.Piccadilly, "status", 12, "carriages", updateTime));

        Set<DepartureDTO> list = new TreeSet<>();
        list.add(departureDTOA);
        list.add(departureDTOB);
        list.add(departureDTOC);

        DepartureDTO[] elements = list.toArray(new DepartureDTO[3]);
        assertEquals("Bury", elements[0].getDestination());
        assertEquals("Deansgate-Castlefield", elements[1].getDestination());
        assertEquals("Piccadilly", elements[2].getDestination());

    }
}
