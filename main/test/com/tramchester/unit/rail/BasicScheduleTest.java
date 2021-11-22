package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicScheduleTest {

    @Test
    void shouldParseBasicScheduleRecord() {
        String line = "BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P";

        BasicSchedule basicSchedule = BasicSchedule.parse(line);

        assertEquals(BasicSchedule.TransactionType.N, basicSchedule.getTransactionType());
    }
}
