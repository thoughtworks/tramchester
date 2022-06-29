package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TerminatingLocation;
import com.tramchester.dataimport.rail.records.reference.LocationActivityCode;
import com.tramchester.domain.time.TramTime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TerminatingLocationTest {

    // LTWLWYNGC 1918 19184     TF
    // LTUPMNLT 21022H1023      TF
    // LTDRBY    0825 0825   BUSTF
    // 0123456789012345678901234567890

    @Test
    void shouldParseRecord() {
        String text = "LTWLWYNGC 1918 19184     TF";

        TerminatingLocation terminatingLocation = parseWithPadding(text);

        assertEquals("WLWYNGC", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(19,18), terminatingLocation.getArrival());
        assertEquals("4", terminatingLocation.getPlatform());
        assertEquals("", terminatingLocation.getPath());
        assertFalse(terminatingLocation.getActivity().isEmpty());
        assertTrue(terminatingLocation.getActivity().contains(LocationActivityCode.TrainFinishes));
    }

    @Test
    void shouldParseRecordFromFile() {
        String text = "LTUPMNLT 21022H1023      TF";

        TerminatingLocation terminatingLocation = parseWithPadding(text);

        assertEquals("UPMNLT", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(10,23), terminatingLocation.getArrival());
        assertEquals("", terminatingLocation.getPlatform());
        assertEquals("", terminatingLocation.getPath());
        assertFalse(terminatingLocation.getActivity().isEmpty());
        assertTrue(terminatingLocation.getActivity().contains(LocationActivityCode.TrainFinishes));
    }

    @Test
    void shouldParseRecordBus() {
        String text =  "LTDRBY    0825 0825   BUSTF           ";

        TerminatingLocation terminatingLocation = parseWithPadding(text);

        assertEquals("DRBY", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(8,25), terminatingLocation.getArrival());
        assertEquals("", terminatingLocation.getPlatform());
        assertEquals("BUS", terminatingLocation.getPath());
        assertFalse(terminatingLocation.getActivity().isEmpty());
        assertTrue(terminatingLocation.getActivity().contains(LocationActivityCode.TrainFinishes));
    }

    @Test
    void shouldParseFinalStageOfLondonUndergroundRoute() {
        String text = "LTLILBDGE 2030 2030      TF               ";

        TerminatingLocation terminatingLocation = parseWithPadding(text);

        assertEquals("LILBDGE", terminatingLocation.getTiplocCode());
        assertEquals(TramTime.of(20,30), terminatingLocation.getArrival());
        assertEquals(TramTime.of(20,30), terminatingLocation.getDeparture());
        assertEquals("", terminatingLocation.getPlatform());
        assertFalse(terminatingLocation.getActivity().isEmpty());
        assertTrue(terminatingLocation.getActivity().contains(LocationActivityCode.TrainFinishes));

    }

    @Test
    void shouldParseTerminatingStageNotBeingFlaggedAsDropoff() {
        String text = "LTSTIRLNG 2355 23559     TFRM                                                  ";

        TerminatingLocation terminatingLocation = parseWithPadding(text);
        assertFalse(terminatingLocation.getActivity().isEmpty());
        assertTrue(terminatingLocation.getActivity().contains(LocationActivityCode.TrainFinishes));
        assertTrue(terminatingLocation.getActivity().contains(LocationActivityCode.StopsForReversingMoveOrDriverChangesEnds));
    }

    @NotNull
    private TerminatingLocation parseWithPadding(String text) {
        String toParse = text;
        int currentLen = text.length();
        if (currentLen<80) {
            int padding = 80 - currentLen;
            toParse = toParse.concat(" ".repeat(padding));
        }
        return TerminatingLocation.parse(toParse);
    }
}
