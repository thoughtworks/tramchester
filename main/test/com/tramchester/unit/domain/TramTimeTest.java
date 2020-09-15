package com.tramchester.unit.domain;

import com.tramchester.domain.time.TimeWindow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramStations;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class TramTimeTest {

    @Test
    void shouldCreateTramTime() {
        TramTime timeA = TramTime.of(11,23);
        assertEquals(11, timeA.getHourOfDay());
        assertEquals(23, timeA.getMinuteOfHour());
    }

    @Test
    void shouldHaveEquality() {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                TramTime tramTime = TramTime.of(hour, minute);
                assertEquals(TramTime.of(hour, minute), tramTime);
                assertNotEquals(TramTime.of(23-hour, 59-minute), tramTime);
            }
        }
        assertNotEquals(TramTime.of(11,42), TramTime.nextDay(11,42));
        assertNotEquals(TramTime.nextDay(11,42), TramTime.of(11,42));
    }

    @Test
    void shouldParseHMS() {
        checkCorrectTimePresent(TramTime.parse("11:23:00"), 11, 23, false);
        checkCorrectTimePresent(TramTime.parse("00:15:00"), 0, 15, false);
        checkCorrectTimePresent(TramTime.parse("23:35:00"), 23, 35, false);
    }

    @Test
    void shouldParseHM() {
        checkCorrectTimePresent(TramTime.parse("11:23"), 11, 23, false);
        checkCorrectTimePresent(TramTime.parse("00:15"), 0, 15, false);
        checkCorrectTimePresent(TramTime.parse("23:35"), 23, 35, false);
        checkCorrectTimePresent(TramTime.parse("23:47"), 23, 47, false);
    }

    @Test
    void shouldParseNextDayAsPerGTFS() {
        checkCorrectTimePresent(TramTime.parse("24:35"), 0, 35, true);
        checkCorrectTimePresent(TramTime.parse("25:47"), 1, 47, true);
        checkCorrectTimePresent(TramTime.parse("26:42"), 2, 42, true);
    }

    @Test
    void shouldParseNextDayPlus24() {
        checkCorrectTimePresent(TramTime.parse("0:35+24"), 0, 35, true);
        checkCorrectTimePresent(TramTime.parse("1:47+24"), 1, 47, true);
        checkCorrectTimePresent(TramTime.parse("2:42+24"), 2, 42, true);
    }

    @Test
    void shouldParseEmptyIfInvalid() {
        Assertions.assertFalse(TramTime.parse("49:12").isPresent());
        Assertions.assertFalse(TramTime.parse("12:99").isPresent());
    }

    @Test
    void shouldFormatCorrectly() {
        TramTime time = TramTime.of(18,56);
        assertEquals("18:56",time.toPattern());
        assertEquals("18:56",time.serialize());

        TramTime nextDay = TramTime.nextDay(11,42);
        assertEquals("11:42",nextDay.toPattern());
        assertEquals("11:42+24",nextDay.serialize());
    }

    @Test
    void shouldBeComparableDuringDaySameHour() {
        TramTime timeA = TramTime.of(12, 4);
        TramTime timeB =  TramTime.of(12, 3);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = TramTime.of(12, 3);
        assertEquals(0, timeC.compareTo(timeB));
        assertEquals(0, timeB.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldBeComparableDuringDayDifferentHour() {
        TramTime timeA = TramTime.of(13, 1);
        TramTime timeB =  TramTime.of(12, 3);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldBeComparableAcrossMidnight() {
        TramTime timeA = TramTime.nextDay(0,10);
        TramTime timeB =  TramTime.of(23,10);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = TramTime.nextDay(0,10);
        assertEquals(0, timeC.compareTo(timeA));
        assertEquals(0, timeA.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldTestTimeWindowsIntervalNearMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(0,1),60);
        TramTime firstStopDepart = TramTime.of(23,9);
        TramTime secondStopArrival = TramTime.of(23, 27);
        Assertions.assertFalse(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldTestTimeWindowsIntervalAfterMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(0,1),60);
        TramTime firstStopDepart = TramTime.of(0,9);
        TramTime secondStopArrival = TramTime.of(0, 27);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldTestTimeWindowsEarlyMorning() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(1,1),60);
        TramTime firstStopDepart = TramTime.of(1,9);
        TramTime secondStopArrival = TramTime.of(1, 27);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldTestTimeWindowsIntervalBeforeMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(23,10),60);
        TramTime firstStopDepart = TramTime.of(23,20);
        TramTime secondStopArrival = TramTime.of(23, 55);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldTestTimeWindowsIntervalOverMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(23,45),60);
        TramTime firstStopDepart = TramTime.of(23,55);
        TramTime secondStopArrival = TramTime.of(0, 15);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldTestTimeWindows() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(10, 1), 60);
        TramTime firstStopDepart = TramTime.of(10, 30);
        TramTime secondStopArrival = TramTime.of(10, 35);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldTestTimeWindowsNoMatch() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(10,1),60);
        TramTime firstStopDepart = TramTime.of(9,9);
        TramTime secondStopArrival = TramTime.of(9, 27);
        Assertions.assertFalse(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    void shouldOrderTramTimesCorrectlyOverMidnight() {
        TramTime timeA = TramTime.nextDay(0,10);
        TramTime timeB =  TramTime.of(23,10); // show first

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldOrderTramTimesNearMidnight() {
        TramTime timeA = TramTime.of(23,47);
        TramTime timeB =  TramTime.of(23,23); // show first

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldCheckIfDepartsAfter() {
        TramTime timeA = TramTime.of(LocalTime.of(12,15));
        TramTime timeB =  TramTime.of(LocalTime.of(9,10));

        assertTrue(timeA.departsAfter(timeB));

        timeA = TramTime.of(LocalTime.of(6,12));
        timeB = TramTime.of(LocalTime.of(6,11));

        assertTrue(timeA.departsAfter(timeB));
        Assertions.assertFalse(timeB.departsAfter(timeA));
    }

    @Test
    void shouldCheckIfDepartsAfterDiffDays() {
        TramTime timeA = TramTime.nextDay(0,10);
        TramTime timeB =  TramTime.of(LocalTime.of(23,10));

        assertTrue(timeA.departsAfter(timeB));

        timeA = TramTime.of(LocalTime.of(6,12));
        timeB = TramTime.of(LocalTime.of(6,11));

        assertTrue(timeA.departsAfter(timeB));
        Assertions.assertFalse(timeB.departsAfter(timeA));
    }

    @Test
    void shouldHaveIsBefore() {
        assertTrue(TramTime.of(11,33).isBefore(TramTime.of(12, 0)));
        assertTrue(TramTime.of(0,3).isBefore(TramTime.of(0,30)));
        assertTrue(TramTime.of(11,3).isBefore(TramTime.nextDay(0,30)));

        Assertions.assertFalse(TramTime.of(10,30).isBefore(TramTime.of(10,30)));
        Assertions.assertFalse(TramTime.of(10,30).isBefore(TramTime.of(9,30)));
        Assertions.assertFalse(TramTime.nextDay(0,30).isBefore(TramTime.of(4,0))); // late night
    }

    @Test
    void shouldHaveAfter() {
        assertTrue(TramTime.of(11,44).isAfter(TramTime.of(11, 0)));
        assertTrue(TramTime.nextDay(0,30).isAfter(TramTime.of(23,30)));
        assertTrue(TramTime.nextDay(0,30).isAfter(TramTime.of(21,30)));
        assertTrue(TramTime.of(0,44).isAfter(TramTime.of(0,20)));
        assertTrue(TramTime.of(2,44).isAfter(TramTime.of(2,20)));

        Assertions.assertFalse(TramTime.of(2,30).isAfter(TramTime.of(23,30)));
        Assertions.assertFalse(TramTime.of(4,30).isAfter(TramTime.nextDay(0,30)));
    }

    @Test
    void shouldIfBetweenAccountingForMidnight() {
        TramTime morning = TramTime.of(11,30);

        assertTrue(morning.between(TramTime.of(9,0), TramTime.of(13,0)));
        assertTrue(morning.between(TramTime.of(11,30), TramTime.of(13,0)));
        assertTrue(morning.between(TramTime.of(10,30), TramTime.of(11,30)));

        Assertions.assertFalse(morning.between(TramTime.of(9,0), TramTime.of(11,0)));
        Assertions.assertFalse(morning.between(TramTime.of(12,0), TramTime.of(13,0)));

        assertTrue(morning.between(TramTime.of(5,0), TramTime.nextDay(0,1)));
        assertTrue(morning.between(TramTime.of(5,0), TramTime.nextDay(0,0)));
        assertTrue(morning.between(TramTime.of(5,0), TramTime.nextDay(1,0)));

        TramTime earlyMorning = TramTime.nextDay(0,20);
        assertTrue(earlyMorning.between(TramTime.nextDay(0,1), TramTime.nextDay(0,21)));
        assertTrue(earlyMorning.between(TramTime.nextDay(0,0), TramTime.nextDay(0,21)));
        assertTrue(earlyMorning.between(TramTime.nextDay(0,1), TramTime.nextDay(0,20)));
        assertTrue(earlyMorning.between(TramTime.nextDay(0,0), TramTime.nextDay(0,20)));
        assertTrue(earlyMorning.between(TramTime.of(5,0), TramTime.nextDay(1,20)));
        Assertions.assertFalse(earlyMorning.between(TramTime.nextDay(3,0), TramTime.nextDay(11,20)));
        Assertions.assertFalse(earlyMorning.between(TramTime.of(23,0), TramTime.nextDay(0,15)));

    }

    @Test
    void shouldHaveCorrectDifferenceIncludingTimesAcrossMidnight() {
        TramTime first = TramTime.of(9,30);
        TramTime second = TramTime.of(10,45);

        int result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(75, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(75, result);

        ////
        first = TramTime.nextDay(0,5);
        second = TramTime.of(23,15);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(50, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(50, result);

        ////
        first = TramTime.nextDay(0,5);
        second = TramTime.of(22,59);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(66, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(66, result);

        ////
        first = TramTime.of(23,59);
        second = TramTime.nextDay(1,10);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(71, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(71, result);
    }

    @Test
    void shouldAddMins() {
        TramTime ref = TramTime.of(0,0);
        assertEquals(TramTime.of(0,42), ref.plusMinutes(42));
        assertEquals(TramTime.of(1,42), ref.plusMinutes(42+60));
        assertEquals(TramTime.of(1,43), ref.plusMinutes(42+61));
        assertEquals(TramTime.of(2,42), ref.plusMinutes(42+120));
        assertEquals(TramTime.of(2,43), ref.plusMinutes(42+121));

        ref = TramTime.of(23,10);
        assertEquals(TramTime.of(23,52), ref.plusMinutes(42));
        assertEquals(TramTime.nextDay(0,9), ref.plusMinutes(59));
        assertEquals(TramTime.nextDay(0,52), ref.plusMinutes(42+60));
        assertEquals(TramTime.nextDay(0,53), ref.plusMinutes(42+61));
        assertEquals(TramTime.nextDay(1,52), ref.plusMinutes(42+120));
        assertEquals(TramTime.nextDay(1,53), ref.plusMinutes(42+121));
    }

    @Test
    void shouldAddSubMinsNextDay() {
        TramTime ref = TramTime.nextDay(11,42);
        assertEquals(TramTime.nextDay(11,52), ref.plusMinutes(10));
        assertEquals(TramTime.nextDay(11,32), ref.minusMinutes(10));
    }

    @Test
    void shouldSubstractMins() {
        TramTime reference = TramTime.of(12, 4);
        TramTime result = reference.minusMinutes(30);

        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = TramTime.of(12, 4);
        result = reference.minusMinutes(90);

        assertEquals(10, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = TramTime.nextDay(0, 4);
        result = reference.minusMinutes(30);

        assertTrue(reference.isNextDay());
        assertFalse(result.isNextDay());
        assertEquals(23, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        // day boundry
        reference = TramTime.nextDay(0, 4);
        result = reference.minusMinutes(90);

        assertTrue(reference.isNextDay());
        assertFalse(result.isNextDay());
        assertEquals(22, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    void shouldSubstractMinsViaLocalTime() {
        LocalTime reference = LocalTime.of(12, 4);
        TramTime result = TramTime.of(reference.minusMinutes(30));

        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    void shouldGetCorrectDaySameDay() {
        LocalDate beginDate = TestEnv.testDay();

        TramTime sameDay = TramTime.of(11,42);

        LocalDateTime result = sameDay.toDate(beginDate);
        assertEquals(sameDay.asLocalTime(), result.toLocalTime());
        assertEquals(beginDate, result.toLocalDate());
    }

    @Test
    void shouldGetCorrectDayNextDay() {
        LocalDate beginDate = TestEnv.testDay();

        TramTime sameDay = TramTime.nextDay(11,42);

        LocalDateTime result = sameDay.toDate(beginDate);
        assertEquals(sameDay.asLocalTime(), result.toLocalTime());
        assertEquals(beginDate.plusDays(1), result.toLocalDate());
    }

    private void checkCorrectTimePresent(Optional<TramTime> resultA, int hours, int minutes, boolean nextDay) {
        assertTrue(resultA.isPresent());
        TramTime time = resultA.get();
        assertEquals(hours, time.getHourOfDay());
        assertEquals(minutes, time.getMinuteOfHour());
        assertEquals(nextDay, time.isNextDay());
    }
}
