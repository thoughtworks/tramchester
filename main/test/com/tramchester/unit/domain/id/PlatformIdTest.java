package com.tramchester.unit.domain.id;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.PlatformId;
import com.tramchester.domain.places.Station;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlatformIdTest {

    @Test
    void shouldRemoveStationFromPlatformNumberIfPresent() {
        IdFor<Station> stationId = TramStations.ImperialWarMuseum.getId();
        String platformNumber = "9400ZZMAIWM1";
        PlatformId platformId = PlatformId.createId(stationId, platformNumber);

        assertEquals("1", platformId.getNumber());
    }

    @Test
    void shouldNotRemoveStationFromPlatformNumberIfNoMatch() {
        IdFor<Station> stationId = TramStations.ImperialWarMuseum.getId();
        String platformNumber = "42";
        PlatformId platformId = PlatformId.createId(stationId, platformNumber);

        assertEquals("42", platformId.getNumber());
    }
}
