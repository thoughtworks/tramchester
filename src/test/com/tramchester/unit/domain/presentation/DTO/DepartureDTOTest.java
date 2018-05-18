package com.tramchester.unit.domain.presentation.DTO;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static junit.framework.TestCase.assertEquals;

public class DepartureDTOTest {

    @Test
    public void shouldCreateFromDueTramAndLocation() {
        LocalTime updateTime = LocalTime.now();

        String location = "aPlace";
        DueTram dueTram = new DueTram("destination", "status", 42, "carriages", updateTime);
        DepartureDTO departureDTO = new DepartureDTO(location, dueTram);

        assertEquals("aPlace", departureDTO.getFrom());
        assertEquals("destination", departureDTO.getDestination());
        assertEquals("status", departureDTO.getStatus());
        assertEquals("carriages", departureDTO.getCarriages());
        assertEquals(TramTime.create(updateTime.plusMinutes(42)), departureDTO.getWhen());
    }

    @Test
    public void shouldCompareBasedOnWhenTramDue() {
        LocalTime updateTime = LocalTime.now();

        DepartureDTO departureDTOA = new DepartureDTO("station", new DueTram("destinationA", "status", 5, "carriages", updateTime));
        DepartureDTO departureDTOB = new DepartureDTO("station", new DueTram("destinationB", "status", 3, "carriages", updateTime));
        DepartureDTO departureDTOC = new DepartureDTO("station", new DueTram("destinationC", "status", 12, "carriages", updateTime));

        Set<DepartureDTO> list = new TreeSet<>();
        list.add(departureDTOA);
        list.add(departureDTOB);
        list.add(departureDTOC);

        DepartureDTO[] elements = list.toArray(new DepartureDTO[3]);
        assertEquals("destinationB", elements[0].getDestination());
        assertEquals("destinationA", elements[1].getDestination());
        assertEquals("destinationC", elements[2].getDestination());

    }
}
