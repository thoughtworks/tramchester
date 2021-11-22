package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.TIPLOCInsert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TIPLOCInsertTest {

    @Test
    void shouldParseRecord() {
        String line = "TIAACHEN 00081601LAACHEN                    00005   0";

        TIPLOCInsert result = TIPLOCInsert.parse(line);

        assertEquals("AACHEN", result.getTiplocCode());
    }
}
