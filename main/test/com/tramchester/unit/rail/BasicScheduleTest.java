package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicScheduleTest {

    // BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P
    // 01234567890123456789012345678901234567890123456789012345678901234567890123456789
    // 00000000001111111111222222222233333333334444444444555555555566666666667777777777

    @Test
    void shouldParseBasicScheduleRecord() {
        ProvidesNow providesNow = new ProvidesLocalNow();

        String line = "BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P";

        // BS N C53290 170524 170920 0000001
        BasicSchedule basicSchedule = BasicSchedule.parse(line, providesNow);

        assertEquals(BasicSchedule.TransactionType.N, basicSchedule.getTransactionType());
        assertEquals("C53290", basicSchedule.getUniqueTrainId());
        assertEquals(LocalDate.of(2017, 5, 24), basicSchedule.getStartDate());
        assertEquals(LocalDate.of(2017, 9, 20), basicSchedule.getEndDate());

        Set<DayOfWeek> daysOfWeek = basicSchedule.getDaysOfWeek();
        assertEquals(1, daysOfWeek.size());
        assertTrue(daysOfWeek.contains(DayOfWeek.SUNDAY));

    }
}
