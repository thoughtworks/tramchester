package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.BasicSchedule;
import com.tramchester.dataimport.rail.records.RailRecordTransactionType;
import com.tramchester.dataimport.rail.records.reference.ShortTermPlanIndicator;
import com.tramchester.dataimport.rail.records.reference.TrainCategory;
import com.tramchester.dataimport.rail.records.reference.TrainStatus;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BasicScheduleTest {
    private int century;

    /// see https://wiki.openraildata.com/index.php/CIF_Codes


    @BeforeEach
    void beforeEachTestRuns() {

        century = 20;
    }

    @Test
    void shouldParseBasicScheduleRecord() {
        // BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P
        // 01234567890123456789012345678901234567890123456789012345678901234567890123456789
        // 00000000001111111111222222222233333333334444444444555555555566666666667777777777

        String text = "BSNC532901705241709200000001 POO2T07    124207004 EMU319 100D     B            P";

        BasicSchedule basicSchedule = BasicSchedule.parse(text, century);

        assertEquals(RailRecordTransactionType.New, basicSchedule.getTransactionType());
        assertEquals("C53290", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(TramDate.of(2017, 5, 24), dateRange.getStartDate());
        assertEquals(TramDate.of(2017, 9, 20), dateRange.getEndDate());

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

        BasicSchedule basicSchedule = BasicSchedule.parse(text, century);

        assertEquals(RailRecordTransactionType.New, basicSchedule.getTransactionType());
        assertEquals("G54660", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(TramDate.of(2021, 12, 12), dateRange.getStartDate());
        assertEquals(TramDate.of(2021, 12, 19), dateRange.getEndDate());

        Set<DayOfWeek> daysOfWeek = basicSchedule.getDaysOfWeek();
        assertEquals(1, daysOfWeek.size());
        assertTrue(daysOfWeek.contains(DayOfWeek.SUNDAY));
        assertEquals(ShortTermPlanIndicator.Permanent, basicSchedule.getSTPIndicator());
        assertEquals("0B00", basicSchedule.getTrainIdentity());
        assertEquals(TrainStatus.Bus, basicSchedule.getTrainStatus());
        assertEquals(TrainCategory.BusService, basicSchedule.getTrainCategory());
    }

    @Test
    void shouldParseShipRecord() {
        String text = "BSNP208612112142205140111110 S  0S000074122340000                 S A          P";

        BasicSchedule basicSchedule = BasicSchedule.parse(text, century);

        assertEquals(RailRecordTransactionType.New, basicSchedule.getTransactionType());
        assertEquals("P20861", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(TramDate.of(2021, 12, 14), dateRange.getStartDate());
        assertEquals(TramDate.of(2022, 5, 14), dateRange.getEndDate());

        assertEquals(TrainStatus.Ship, basicSchedule.getTrainStatus());
        assertEquals(TrainCategory.Unknown, basicSchedule.getTrainCategory());

    }

    @Test
    void shouldParseLondonUndergroundRecord() {
        String text = "BSNC611912105162112050000001 POL2I01    124682004 EMU    045                   P";

        BasicSchedule basicSchedule = BasicSchedule.parse(text, century);

        assertEquals(RailRecordTransactionType.New, basicSchedule.getTransactionType());
        assertEquals("C61191", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(TramDate.of(2021, 5, 16), dateRange.getStartDate());
        assertEquals(TramDate.of(2021, 12, 5), dateRange.getEndDate());

        assertEquals(TrainStatus.PassengerAndParcels, basicSchedule.getTrainStatus());
        assertEquals(TrainCategory.LondonUndergroundOrMetroService, basicSchedule.getTrainCategory());
    }

    @Test
    void shouldParseBasicScheduleThatCancelsUnseenService() {
        String text = "BSNX625452301292301290000001 1OO2K26    124782000 EMU    100D                  N";

        BasicSchedule basicSchedule = BasicSchedule.parse(text, century);

        assertEquals("X62545", basicSchedule.getUniqueTrainId());
        DateRange dateRange = basicSchedule.getDateRange();
        assertEquals(TramDate.of(2023, 1, 29), dateRange.getStartDate());
        assertEquals(TramDate.of(2023, 1, 29), dateRange.getEndDate());

        assertEquals(TrainStatus.STPPassengerParcels, basicSchedule.getTrainStatus());
        assertEquals(TrainCategory.OrdinaryPassenger, basicSchedule.getTrainCategory());

        assertEquals(RailRecordTransactionType.New, basicSchedule.getTransactionType());

        assertEquals(ShortTermPlanIndicator.New, basicSchedule.getSTPIndicator());
    }
}
