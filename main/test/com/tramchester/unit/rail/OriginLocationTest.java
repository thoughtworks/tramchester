package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.OriginLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OriginLocationTest {

    @Test
    void shouldParseOriginLocation() {
        String line = "LOLINCLNC 1237 12384A        TB";

        OriginLocation originLocation = OriginLocation.parse(line);

        assertEquals("LINCLNC", originLocation.getTiplocCode());
        assertEquals(TramTime.of(12, 38), originLocation.getPublicDepartureTime());
        assertEquals("4A", originLocation.getPlatform());
    }
}
