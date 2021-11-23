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
    }

    @Test
    void shouldParseNonStopRecord() {
        String line = "LIBATRSPJ           2125H00000000                          H";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);

        assertEquals("BATRSPJ", intermediateLocation.getTiplocCode());
    }

    @Test
    void shouldParseTiplocCorrectlyWhenNoSpaceAfter() {
        String line = "LIKEWGRDN22047 2047H     204720471        T";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);

        assertEquals(7, intermediateLocation.getTiplocCode().length());
        assertEquals("KEWGRDN", intermediateLocation.getTiplocCode());
    }

    @Test
    void shouldParseCorrectlyWhenNoPublicArrivalOrDepart() {
        // LIFARE825 1242H1246H     00000000         OPA

        // from TI insert record
        // TIFARE82500590001EFAREHAM SIGNAL E825       86238   0

        // from railway codes
        // Location 	            CRS 	NLC 	    TIPLOC 	    STANME 	    STANOX
        // Fareham Signal E825              590001      FARE825     FAREHM825   86238

        String line = "LIFARE825 1242H1246H     00000000         OPA";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(line);

        assertEquals(7, intermediateLocation.getTiplocCode().length());
        assertEquals("FARE825", intermediateLocation.getTiplocCode());
    }
}
