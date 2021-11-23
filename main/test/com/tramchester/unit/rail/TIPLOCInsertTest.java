package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TIPLOCInsert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TIPLOCInsertTest {

    @Test
    void shouldParseSimpleRecord() {
        String line = "TIAACHEN 00081601LAACHEN                    00005   0";

        TIPLOCInsert result = TIPLOCInsert.parse(line);

        assertEquals("AACHEN", result.getTiplocCode());
    }

    @Test
    void shouldParseRecordFromFile() {

        // TIBATRSPJ48528862ZBATTERSEA PIER JN.        87199   0
        // 01234567890123456789012345678901234567890123456789012
        // 0         1         2         3         4         5

        String line = "TIBATRSPJ48528862ZBATTERSEA PIER JN.        87199   0";

        TIPLOCInsert result = TIPLOCInsert.parse(line);

        assertEquals("BATRSPJ", result.getTiplocCode());
        assertEquals("BATTERSEA PIER JN.", result.getName());
    }
}
