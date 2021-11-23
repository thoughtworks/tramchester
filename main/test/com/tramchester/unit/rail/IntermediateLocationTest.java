package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.IntermediateLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntermediateLocationTest {

    @Test
    void shouldParseRecord() {
        String line = "LINEWSGAT 1852H1853      18531854123      T";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);

        assertEquals("NEWSGAT", intermediateLocation.getTiplocCode());
        assertEquals(TramTime.of(18,53), intermediateLocation.getPublicArrival());
        assertEquals(TramTime.of(18,54), intermediateLocation.getPublicDeparture());
        assertEquals("123", intermediateLocation.getPlatform());
        assertTrue(intermediateLocation.hasCallingTimes());
    }

    @Test
    void shouldParseNonStopRecord() {
        String line = "LIBATRSPJ           2125H00000000                          H";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);

        assertEquals("BATRSPJ", intermediateLocation.getTiplocCode());
        assertFalse(intermediateLocation.hasCallingTimes());
    }

    @Test
    void shouldParseTiplocCorrectlyWhenNoSpaceAfter() {
        String line = "LIKEWGRDN22047 2047H     204720471        T";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);

        assertEquals(7, intermediateLocation.getTiplocCode().length());
        assertEquals("KEWGRDN", intermediateLocation.getTiplocCode());
    }
}
