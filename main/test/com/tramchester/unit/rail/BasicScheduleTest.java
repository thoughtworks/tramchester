package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.dataimport.rail.records.reference.ShortTermPlanIndicator;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicScheduleTest {

    /// see https://wiki.openraildata.com/index.php/CIF_Codes

    private ProvidesNow providesNow;

    @BeforeEach
    void beforeEachTestRuns() {
        providesNow = new ProvidesLocalNow();
    }

    @Test
    void shouldParseBasicScheduleRecord() {
        // BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P
        // 01234567890123456789012345678901234567890123456789012345678901234567890123456789
        // 00000000001111111111222222222233333333334444444444555555555566666666667777777777

        String text = "BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P";

        BasicSchedule basicSchedule = BasicSchedule.parse(text, providesNow);

        assertEquals(BasicSchedule.TransactionType.N, basicSchedule.getTransactionType());
        assertEquals("C53290", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(LocalDate.of(2017, 5, 24), dateRange.getStartDate());
        assertEquals(LocalDate.of(2017, 9, 20), dateRange.getEndDate());

        Set<DayOfWeek> daysOfWeek = basicSchedule.getDaysOfWeek();
        assertEquals(1, daysOfWeek.size());
        assertTrue(daysOfWeek.contains(DayOfWeek.SUNDAY));
        assertEquals(ShortTermPlanIndicator.Permanent, basicSchedule.getSTPIndicator());
        assertEquals("2T07", basicSchedule.getTrainIdentity());
        assertEquals(TrainStatus.PassengerAndParcels, basicSchedule.getTrainStatus());
        assertEquals(TrainCategory.OrdinaryPassenger, basicSchedule.getTrainCategory());
    }

    @Test
    void shouldParseBusServiceRecord() {
        String text = "BSNG546602112122112190000001 BBS0B0028  125527005                              P";

        BasicSchedule basicSchedule = BasicSchedule.parse(text, providesNow);

        assertEquals(BasicSchedule.TransactionType.N, basicSchedule.getTransactionType());
        assertEquals("G54660", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(LocalDate.of(2021, 12, 12), dateRange.getStartDate());
        assertEquals(LocalDate.of(2021, 12, 19), dateRange.getEndDate());

        Set<DayOfWeek> daysOfWeek = basicSchedule.getDaysOfWeek();
        assertEquals(1, daysOfWeek.size());
        assertTrue(daysOfWeek.contains(DayOfWeek.SUNDAY));
        assertEquals(ShortTermPlanIndicator.Permanent, basicSchedule.getSTPIndicator());
        assertEquals("0B00", basicSchedule.getTrainIdentity());
        assertEquals(TrainStatus.Bus, basicSchedule.getTrainStatus());
        assertEquals(TrainCategory.BusService, basicSchedule.getTrainCategory());
    }
}
