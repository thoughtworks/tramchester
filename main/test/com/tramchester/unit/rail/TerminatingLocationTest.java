package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TerminatingLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TerminatingLocationTest {

    // LTWLWYNGC 1918 19184     TF
    // LTUPMNLT 21022H1023      TF
    // LTDRBY    0825 0825   BUSTF
    // 0123456789012345678901234567890

    @Test
    void shouldParseRecord() {
        String text = "LTWLWYNGC 1918 19184     TF";

        TerminatingLocation terminatingLocation = TerminatingLocation.parse(text);

        assertEquals("WLWYNGC", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(19,18), terminatingLocation.getArrival());
        assertEquals("4", terminatingLocation.getPlatform());
        assertEquals("", terminatingLocation.getPath());
    }

    @Test
    void shouldParseRecordFromFile() {
        String text = "LTUPMNLT 21022H1023      TF";

        TerminatingLocation terminatingLocation = TerminatingLocation.parse(text);

        assertEquals("UPMNLT", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(10,23), terminatingLocation.getArrival());
        assertEquals("", terminatingLocation.getPlatform());
        assertEquals("", terminatingLocation.getPath());
    }

    @Test
    void shouldParseRecordBus() {
        String text =  "LTDRBY    0825 0825   BUSTF           ";

        TerminatingLocation terminatingLocation = TerminatingLocation.parse(text);

        assertEquals("DRBY", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(8,25), terminatingLocation.getArrival());
        assertEquals("", terminatingLocation.getPlatform());
        assertEquals("BUS", terminatingLocation.getPath());
    }
}
