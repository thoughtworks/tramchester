package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.OriginLocation;
import com.tramchester.domain.time.TramTime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OriginLocationTest {

    @Test
    void shouldParseOriginLocationFromExample() {
        // LOLINCLNC 1237 12384A        TB
        // 0123456789012345678901234567890

        String text = "LOLINCLNC 1237 12384A        TB";

        OriginLocation originLocation = OriginLocation.parse(text);

        assertEquals("LINCLNC", originLocation.getTiplocCode());
        assertEquals(TramTime.of(12, 38), originLocation.getPublicDeparture());
        assertEquals("4A", originLocation.getPlatform());
        assertEquals("", originLocation.getLine());
    }

    @Test
    void shouldParseOriginLocationFromFile() {
        // LODRBY    1749 17494B DTS    TBT
        // 0123456789012345678901234567890

        String text = "LODRBY    1749 17494B DTS    TBT";

        OriginLocation originLocation = OriginLocation.parse(text);

        assertEquals("DRBY", originLocation.getTiplocCode());
        assertEquals(TramTime.of(17, 49), originLocation.getPublicDeparture());
        assertEquals("4B", originLocation.getPlatform());
        assertEquals("DTS", originLocation.getLine());
    }

    @Test
    void shouldParseApparentBusLocation() {
        // LODRBY    1749 17494B DTS    TBT
        // LOMINEBUT 1845 1845   BUS    TB
        // 0123456789012345678901234567890

        String text = "LOMINEBUT 1845 1845   BUS    TB     ";

        OriginLocation originLocation = OriginLocation.parse(text);

        assertEquals("MINEBUT", originLocation.getTiplocCode());
        assertEquals(TramTime.of(18, 45), originLocation.getPublicDeparture());
        assertEquals("", originLocation.getPlatform());
        assertEquals("BUS", originLocation.getLine());
    }
}
