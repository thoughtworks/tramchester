package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.IntermediateLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntermediateLocationTest {

    @Test
    void shouldParseRecord() {
        String line = "LINEWSGAT 1852H1853      18531854123      T";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);
        assertEquals("NEWSGAT", intermediateLocation.getTiplocCode());
        assertEquals(TramTime.of(18,53), intermediateLocation.getPublicArrival());
        assertEquals(TramTime.of(18,54), intermediateLocation.getPublicDeparture());
        assertEquals("123", intermediateLocation.getPlatform());

    }
}
