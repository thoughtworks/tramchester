package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.OriginLocation;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OriginLocationTest {

    @Test
    void shouldParseOriginLocationFromExample() {
        // LOLINCLNC 1237 12384A        TB
        // 0123456789012345678901234567890

        String text = "LOLINCLNC 1237 12384A        TB";

        OriginLocation originLocation = parseWithPadding(text);

        assertEquals("LINCLNC", originLocation.getTiplocCode());
        assertEquals(TramTime.of(12, 38), originLocation.getDeparture());
        assertEquals("4A", originLocation.getPlatform());
        assertEquals("", originLocation.getLine());
        assertFalse(originLocation.getActivity().isEmpty());
        assertTrue(originLocation.getActivity().contains(LocationActivityCode.TrainBegins));

    }

    @Test
    void shouldParseOriginLocationFromFile() {
        // LODRBY    1749 17494B DTS    TBT
        // 0123456789012345678901234567890

        String text = "LODRBY    1749 17494B DTS    TBT";

        OriginLocation originLocation = parseWithPadding(text);

        assertEquals("DRBY", originLocation.getTiplocCode());
        assertEquals(TramTime.of(17, 49), originLocation.getDeparture());
        assertEquals("4B", originLocation.getPlatform());
        assertEquals("DTS", originLocation.getLine());
        assertFalse(originLocation.getActivity().isEmpty());
        assertTrue(originLocation.getActivity().contains(LocationActivityCode.TrainBegins));
        assertTrue(originLocation.getActivity().contains(LocationActivityCode.StopsToTakeUpAndSetDownPassengers));
    }

    @Test
    void shouldParseApparentBusLocation() {
        // LODRBY    1749 17494B DTS    TBT
        // LOMINEBUT 1845 1845   BUS    TB
        // 0123456789012345678901234567890

        String text = "LOMINEBUT 1845 1845   BUS    TB     ";

        OriginLocation originLocation = parseWithPadding(text);

        assertEquals("MINEBUT", originLocation.getTiplocCode());
        assertEquals(TramTime.of(18, 45), originLocation.getDeparture());
        assertEquals("", originLocation.getPlatform());
        assertEquals("BUS", originLocation.getLine());
        assertFalse(originLocation.getActivity().isEmpty());
        assertTrue(originLocation.getActivity().contains(LocationActivityCode.TrainBegins));
    }

    @NotNull
    private OriginLocation parseWithPadding(String text) {
        String toParse = text;
        int currentLen = text.length();
        if (currentLen<80) {
            int padding = 80 - currentLen;
            toParse = toParse.concat(" ".repeat(padding));
        }
        return OriginLocation.parse(toParse);
    }
}
