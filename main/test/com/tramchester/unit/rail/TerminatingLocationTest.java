package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TerminatingLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerminatingLocationTest {

    @Test
    void shouldParseRecord() {
        String line = "LTWLWYNGC 1918 19184     TF";

        TerminatingLocation terminatingLocation = TerminatingLocation.parse(line);

        assertEquals("WLWYNGC", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(19,18), terminatingLocation.getPublicArrivalTime());
        assertEquals("4", terminatingLocation.getPlatform());
    }
}
