package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.IntermediateLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntermediateLocationTest {

    @Test
    void shouldParseRecord() {
        String text = "LINEWSGAT 1852H1853      18531854123      T";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(text);

        assertEquals("NEWSGAT", intermediateLocation.getTiplocCode());

        assertEquals(TramTime.of(18,52), intermediateLocation.getScheduledArrival());
        assertEquals(TramTime.of(18,53), intermediateLocation.getScheduledDeparture());

        assertEquals(TramTime.of(18,53), intermediateLocation.getPublicArrival());
        assertEquals(TramTime.of(18,54), intermediateLocation.getPublicDeparture());

        assertEquals(TramTime.of(18,53), intermediateLocation.getArrival());
        assertEquals(TramTime.of(18,54), intermediateLocation.getDeparture());
        assertEquals("123", intermediateLocation.getPlatform());
        assertFalse(intermediateLocation.isPassingRecord());
    }

    @Test
    void shouldParsePassingRecord() {
        // i.e. record records passing a location but not calling at

        //             LIBATRSPJ           0112H00000000
        String text = "LIBATRSPJ           2125H00000000                          H";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(text);

        assertEquals("BATRSPJ", intermediateLocation.getTiplocCode());
        assertTrue(intermediateLocation.isPassingRecord());
        assertEquals(TramTime.of(21,25), intermediateLocation.getPassingTime());
        assertFalse(intermediateLocation.getArrival().isValid());
        assertFalse(intermediateLocation.getDeparture().isValid());

        assertTrue(intermediateLocation.getPlatform().isBlank());
    }



    @Test
    void shouldParseCorrectlyWhenNoPublicArrivalOrDepart() {
        // LIFARE825 1242H1246H     00000000         OPA

        // from TI insert record
        // TIFARE82500590001EFAREHAM SIGNAL E825       86238   0

        // from railway codes
        // Location 	            CRS 	NLC 	    TIPLOC 	    STANME 	    STANOX
        // Fareham Signal E825              590001      FARE825     FAREHM825   86238

        //             01234567890123456789012345678901234567890123456
        //             0         1         2         3         4
        //             LIMOTHRWL 2349H2359H     000023571        U
        String text = "LIFARE825 1242H1246H     00000000         OPA";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(text);

        assertEquals(7, intermediateLocation.getTiplocCode().length());
        assertEquals("FARE825", intermediateLocation.getTiplocCode());
        assertFalse(intermediateLocation.isPassingRecord());

        assertEquals(TramTime.of(0,0),intermediateLocation.getPublicArrival());
        assertEquals(TramTime.of(0,0),intermediateLocation.getPublicDeparture());

        assertEquals(TramTime.of(12,42),intermediateLocation.getScheduledArrival());
        assertEquals(TramTime.of(12,46),intermediateLocation.getScheduledDeparture());

        assertEquals(TramTime.of(12,42),intermediateLocation.getArrival());
        assertEquals(TramTime.of(12,46),intermediateLocation.getDeparture());

    }

    @Test
    void shouldParseTiplocCorrectlyWhenNoSpaceAfter() {
        String text = "LIKEWGRDN22047 2047H     204720471        T";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(text);

        assertEquals(7, intermediateLocation.getTiplocCode().length());
        assertEquals("KEWGRDN", intermediateLocation.getTiplocCode());
        assertFalse(intermediateLocation.isPassingRecord());

        assertEquals(TramTime.of(20,47), intermediateLocation.getPublicArrival());
        assertEquals(TramTime.of(20,47), intermediateLocation.getPublicDeparture());

        assertEquals("1", intermediateLocation.getPlatform());

    }

    @Test
    void shouldParseLondonUnderground() {
        String text = "LIKEWGRDN           2010 000000001                            ";

        IntermediateLocation intermediateLocation = IntermediateLocation.parse(text);

        assertEquals(7, intermediateLocation.getTiplocCode().length());
        assertEquals("KEWGRDN", intermediateLocation.getTiplocCode());

        assertFalse(intermediateLocation.getPublicArrival().isValid());
        assertFalse(intermediateLocation.getPublicDeparture().isValid());

        assertTrue(intermediateLocation.isPassingRecord());
        assertEquals(TramTime.of(20,10), intermediateLocation.getPassingTime());

        assertEquals("1", intermediateLocation.getPlatform());


    }
}
