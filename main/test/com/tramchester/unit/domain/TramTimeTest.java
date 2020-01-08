package com.tramchester.unit.domain;

import com.tramchester.domain.TimeWindow;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import org.junit.Test;

import java.time.LocalTime;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.*;

public class TramTimeTest {

    @Test
    public void shouldCreateTramTime() {
        TramTime timeA = TramTime.of(11,23);
        assertEquals(11, timeA.getHourOfDay());
        assertEquals(23, timeA.getMinuteOfHour());
    }

    @Test
    public void shouldHaveEquality() {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                TramTime tramTime = TramTime.of(hour, minute);
                assertEquals(TramTime.of(hour, minute), tramTime);
                assertNotEquals(TramTime.of(23-hour, 59-minute), tramTime);
            }
        }
    }

    @Test
    public void shouldCalculateMinsOfDay() {
        TramTime midnight = TramTime.of(0,0);
        assertEquals(0, midnight.minutesOfDay());

        TramTime timeA = TramTime.of(11,23);
        assertEquals((11*60)+23, timeA.minutesOfDay());

        TramTime timeB = TramTime.of(23,45);
        assertEquals((24*60)-15, timeB.minutesOfDay());
    }

    @Test
    public void shouldParseHMS() {
        checkCorrectTimePresent(TramTime.parse("11:23:00"), 11, 23);
        checkCorrectTimePresent(TramTime.parse("00:15:00"), 00, 15);
        checkCorrectTimePresent(TramTime.parse("23:35:00"), 23, 35);
    }

    @Test
    public void shouldParseHM() {
        checkCorrectTimePresent(TramTime.parse("11:23"), 11, 23);
        checkCorrectTimePresent(TramTime.parse("00:15"), 00, 15);
        checkCorrectTimePresent(TramTime.parse("23:35"), 23, 35);
        checkCorrectTimePresent(TramTime.parse("23:47"), 23, 47);

    }

    @Test
    public void shouldParseEmptyIfInvalid() {
        assertFalse(TramTime.parse("43:12").isPresent());
        assertFalse(TramTime.parse("12:99").isPresent());
    }

    @Test
    public void shouldFormatCorrectly() {
        TramTime time = TramTime.of(18,56);

        assertEquals("18:56",time.toPattern());
        assertEquals("18:56:00", time.tramDataFormat());
    }

    @Test
    public void shouldBeComparableDuringDaySameHour() {
        TramTime timeA = TramTime.of(12,04);
        TramTime timeB =  TramTime.of(12,03);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = TramTime.of(12,03);
        assertEquals(0, timeC.compareTo(timeB));
        assertEquals(0, timeB.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldBeComparableDuringDayDifferentHour() {
        TramTime timeA = TramTime.of(13,01);
        TramTime timeB =  TramTime.of(12,03);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldBeComparableAcrossMidnight() {
        TramTime timeA = TramTime.of(00,10);
        TramTime timeB =  TramTime.of(23,10);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = TramTime.of(00,10);
        assertEquals(0, timeC.compareTo(timeA));
        assertEquals(0, timeA.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldTestTimeWindowsIntervalNearMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(0,1),60);
        TramTime firstStopDepart = TramTime.of(23,9);
        TramTime secondStopArrival = TramTime.of(23, 27);
        assertFalse(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldTestTimeWindowsIntervalAfterMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(0,1),60);
        TramTime firstStopDepart = TramTime.of(0,9);
        TramTime secondStopArrival = TramTime.of(0, 27);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldTestTimeWindowsEarlyMorning() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(1,1),60);
        TramTime firstStopDepart = TramTime.of(1,9);
        TramTime secondStopArrival = TramTime.of(1, 27);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldTestTimeWindowsIntervalBeforeMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(23,10),60);
        TramTime firstStopDepart = TramTime.of(23,20);
        TramTime secondStopArrival = TramTime.of(23, 55);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldTestTimeWindowsIntervalOverMidnight() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(23,45),60);
        TramTime firstStopDepart = TramTime.of(23,55);
        TramTime secondStopArrival = TramTime.of(0, 15);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldTestTimeWindows() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(10, 1), 60);
        TramTime firstStopDepart = TramTime.of(10, 30);
        TramTime secondStopArrival = TramTime.of(10, 35);
        assertTrue(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldTestTimeWindowsNoMatch() {
        TimeWindow timeWindow = new TimeWindow(TramTime.of(10,1),60);
        TramTime firstStopDepart = TramTime.of(9,9);
        TramTime secondStopArrival = TramTime.of(9, 27);
        assertFalse(TramTime.checkTimingOfStops(timeWindow, firstStopDepart, secondStopArrival));
    }

    @Test
    public void shouldOrderTramTimesCorrectlyOverMidnight() {
        TramTime timeA = TramTime.of(00,10);
        TramTime timeB =  TramTime.of(23,10); // show first

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldOrderTramTimesNearMidnight() {
        TramTime timeA = TramTime.of(23,47);
        TramTime timeB =  TramTime.of(23,23); // show first

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldCheckIfDepartsAfter() {
        TramTime timeA = TramTime.of(LocalTime.of(0,10));
        TramTime timeB =  TramTime.of(LocalTime.of(23,10));

        assertTrue(timeA.departsAfter(timeB));

        timeA = TramTime.of(LocalTime.of(6,12));
        timeB = TramTime.of(LocalTime.of(6,11));

        assertTrue(timeA.departsAfter(timeB));
        assertFalse(timeB.departsAfter(timeA));
    }

    @Test
    public void shouldHaveIsBefore() {
        assertTrue(TramTime.of(11,33).isBefore(TramTime.of(12,00)));
        assertTrue(TramTime.of(0,3).isBefore(TramTime.of(0,30)));
        assertTrue(TramTime.of(11,3).isBefore(TramTime.of(0,30)));

        assertFalse(TramTime.of(10,30).isBefore(TramTime.of(10,30)));
        assertFalse(TramTime.of(10,30).isBefore(TramTime.of(9,30)));
        assertFalse(TramTime.of(0,30).isBefore(TramTime.of(4,0))); // late night
    }

    @Test
    public void shouldHaveAfter() {
        assertTrue(TramTime.of(11,44).isAfter(TramTime.of(11,00)));
        assertTrue(TramTime.of(0,30).isAfter(TramTime.of(23,30)));
        assertTrue(TramTime.of(0,30).isAfter(TramTime.of(21,30)));
        assertTrue(TramTime.of(00,44).isAfter(TramTime.of(00,20)));
        assertTrue(TramTime.of(2,44).isAfter(TramTime.of(2,20)));

        assertFalse(TramTime.of(2,30).isAfter(TramTime.of(23,30)));
        assertFalse(TramTime.of(4,30).isAfter(TramTime.of(0,30)));
    }

    @Test
    public void shouldIfBetweenAccountingForMidnight() {
        TramTime morning = TramTime.of(11,30);

        assertTrue(morning.between(TramTime.of(9,0), TramTime.of(13,0)));
        assertTrue(morning.between(TramTime.of(11,30), TramTime.of(13,0)));
        assertTrue(morning.between(TramTime.of(10,30), TramTime.of(11,30)));

        assertFalse(morning.between(TramTime.of(9,0), TramTime.of(11,0)));
        assertFalse(morning.between(TramTime.of(12,0), TramTime.of(13,0)));

        assertTrue(morning.between(TramTime.of(5,0), TramTime.of(0,1)));
        assertTrue(morning.between(TramTime.of(5,0), TramTime.of(0,0)));
        assertTrue(morning.between(TramTime.of(5,0), TramTime.of(1,0)));

        TramTime earlyMorning = TramTime.of(0,20);
        assertTrue(earlyMorning.between(TramTime.of(0,1), TramTime.of(0,21)));
        assertTrue(earlyMorning.between(TramTime.of(0,0), TramTime.of(0,21)));
        assertTrue(earlyMorning.between(TramTime.of(0,1), TramTime.of(0,20)));
        assertTrue(earlyMorning.between(TramTime.of(0,0), TramTime.of(0,20)));
        assertTrue(earlyMorning.between(TramTime.of(5,0), TramTime.of(1,20)));
        assertFalse(earlyMorning.between(TramTime.of(3,0), TramTime.of(11,20)));
        assertFalse(earlyMorning.between(TramTime.of(23,0), TramTime.of(0,15)));

    }

    @Test
    public void shouldHaveCorrectDifferenceIncludingTimesAcrossMidnight() throws TramchesterException {
        TramTime first = TramTime.create(9,30);
        TramTime second = TramTime.create(10,45);

        int result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(75, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(75, result);

        ////
        first = TramTime.create(00,5);
        second = TramTime.create(23,15);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(50, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(50, result);

        ////
        first = TramTime.create(00,5);
        second = TramTime.create(22,59);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(66, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(66, result);

        ////
        first = TramTime.create(23,59);
        second = TramTime.create(1,10);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(71, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(71, result);
    }

    @Test
    public void shouldAddMins() {
        TramTime ref = TramTime.of(0,0);
        assertEquals(TramTime.of(0,42), ref.plusMinutes(42));
        assertEquals(TramTime.of(1,42), ref.plusMinutes(42+60));
        assertEquals(TramTime.of(1,43), ref.plusMinutes(42+61));
        assertEquals(TramTime.of(2,42), ref.plusMinutes(42+120));
        assertEquals(TramTime.of(2,43), ref.plusMinutes(42+121));

        ref = TramTime.of(23,10);
        assertEquals(TramTime.of(23,52), ref.plusMinutes(42));
        assertEquals(TramTime.of(0,9), ref.plusMinutes(59));
        assertEquals(TramTime.of(0,52), ref.plusMinutes(42+60));
        assertEquals(TramTime.of(0,53), ref.plusMinutes(42+61));
        assertEquals(TramTime.of(1,52), ref.plusMinutes(42+120));
        assertEquals(TramTime.of(1,53), ref.plusMinutes(42+121));
    }

    @Test
    public void shouldSubstractMins() {
        TramTime reference = TramTime.of(12,04);
        TramTime result = reference.minusMinutes(30);

        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = TramTime.of(12,04);
        result = reference.minusMinutes(90);

        assertEquals(10, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = TramTime.of(00,04);
        result = reference.minusMinutes(30);

        assertEquals(23, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = TramTime.of(00,04);
        result = reference.minusMinutes(90);

        assertEquals(22, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    public void shouldSubstractMinsViaLocalTime() {
        LocalTime reference = LocalTime.of(12, 04);
        TramTime result = TramTime.of(reference.minusMinutes(30));

        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    private void checkCorrectTimePresent(Optional<TramTime> resultA, int hours, int minutes) {
        assertTrue(resultA.isPresent());
        TramTime time = resultA.get();
        assertEquals(hours, time.getHourOfDay());
        assertEquals(minutes, time.getMinuteOfHour());
    }
}
