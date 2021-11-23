package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TerminatingLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerminatingLocationTest {

    // LTWLWYNGC 1918 19184     TF
    // LTUPMNLT 21022H1023      TF

    @Test
    void shouldParseRecord() {
        String line = "LTWLWYNGC 1918 19184     TF";

        TerminatingLocation terminatingLocation = TerminatingLocation.parse(line);

        assertEquals("WLWYNGC", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(19,18), terminatingLocation.getPublicArrival());
        assertEquals("4", terminatingLocation.getPlatform());
    }

    @Test
    void shouldParseRecordFromFile() {
        String line = "LTUPMNLT 21022H1023      TF";

        TerminatingLocation terminatingLocation = TerminatingLocation.parse(line);

        assertEquals("UPMNLT", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(10,23), terminatingLocation.getPublicArrival());
        assertEquals("", terminatingLocation.getPlatform());
    }
}
