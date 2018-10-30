package com.tramchester.unit.domain;

import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TramTimeTest {

    @Test
    public void shouldCreateTramTime() throws TramchesterException {
        TramTime timeA = TramTime.create(11,23);
        assertEquals(11, timeA.getHourOfDay());
        assertEquals(23, timeA.getMinuteOfHour());
    }

    @Test
    public void shouldCalculateMinsOfDay() throws TramchesterException {
        TramTime midnight = TramTime.create(0,0);
        assertEquals(24*60, midnight.minutesOfDay());

        TramTime timeA = TramTime.create(11,23);
        assertEquals((11*60)+23, timeA.minutesOfDay());

        TramTime timeB = TramTime.create(23,45);
        assertEquals((24*60)-15, timeB.minutesOfDay());

    }

    @Test
    public void shouldParseHMS() throws TramchesterException {
        checkCorrectTimePresent(TramTime.parse("11:23:00"), 11, 23);
        checkCorrectTimePresent(TramTime.parse("00:15:00"), 00, 15);
        checkCorrectTimePresent(TramTime.parse("23:35:00"), 23, 35);
        checkCorrectTimePresent(TramTime.parse("25:35:00"), 00, 35); // quirk in source tfgm data
    }

    @Test
    public void shouldParseHM() throws TramchesterException {
        checkCorrectTimePresent(TramTime.parse("11:23"), 11, 23);
        checkCorrectTimePresent(TramTime.parse("00:15"), 00, 15);
        checkCorrectTimePresent(TramTime.parse("23:35"), 23, 35);
        checkCorrectTimePresent(TramTime.parse("25:35"), 00, 35); // quirk in source tfgm data
    }

    @Test
    public void shouldParseEmptyIfInvalid() throws TramchesterException {
        assertFalse(TramTime.parse("43:12").isPresent());
        assertFalse(TramTime.parse("12:99").isPresent());
    }

    @Test
    public void shouldFormatCorrectly() throws TramchesterException {
        TramTime time = TramTime.create(18,56);

        assertEquals("18:56",time.toPattern());
        assertEquals("18:56:00", time.tramDataFormat());
    }

    @Test
    public void shouldHaveBefore() throws TramchesterException {
        TramTime timeA = TramTime.create(12,04);
        TramTime timeB =  TramTime.create(12,03);

        assertTrue(timeB.isBefore(timeA));
        assertFalse(timeA.isBefore(timeB));

        timeA = TramTime.create(13,04);
        timeB =  TramTime.create(12,04);

        assertTrue(timeB.isBefore(timeA));
        assertFalse(timeA.isBefore(timeB));
    }

    @Test
    public void shouldHaveAfter() throws TramchesterException {
        TramTime timeA = TramTime.create(12,04);
        TramTime timeB =  TramTime.create(12,03);

        assertTrue(timeA.isAfter(timeB));
        assertFalse(timeB.isAfter(timeA));

        timeA = TramTime.create(13,03);
        timeB =  TramTime.create(12,03);

        assertTrue(timeA.isAfter(timeB));
        assertFalse(timeB.isAfter(timeA));
    }

    @Test
    public void shouldBeComparable() throws TramchesterException {
        TramTime timeA = TramTime.create(12,04);
        TramTime timeB =  TramTime.create(12,03);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = TramTime.create(12,03);
        assertEquals(0, timeC.compareTo(timeB));
        assertEquals(0, timeB.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldHandleAfterMidnightTimesCorrectly() throws TramchesterException {
        TramTime timeA = TramTime.create(00,10);
        TramTime timeB =  TramTime.create(23,10);

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    public void shouldHaveCorrectDifferenceIncludingTimesAcrossMidnight() throws TramchesterException {
        TramTime depart = TramTime.create(9,30);
        TramTime arrive = TramTime.create(10,45);

        int result = TramTime.diffenceAsMinutes(arrive, depart);
        assertEquals(75, result);

        arrive = TramTime.create(00,5);
        depart = TramTime.create(23,15);

        result = TramTime.diffenceAsMinutes(arrive, depart);
        assertEquals(50, result);
    }

    @Test
    public void shouldCreateFromMinsOfDay() throws TramchesterException {
        checkTimeFromMins(210, 3, 30);
        checkTimeFromMins((23*60)+43, 23, 43);
    }

    @Test
    public void shouldCreateFromLocalTime() {
        LocalTime localTime = new LocalTime(14,55);

        TramTime tramTime = TramTime.create(localTime);
        assertEquals(tramTime.getMinuteOfHour(), 55);
        assertEquals(tramTime.getHourOfDay(), 14);
    }

    @Test
    public void shouldTransformToAndFromMinutesEarlyMorning() throws TramchesterException {
        TramTime early = TramTime.create(0,14);

        int minutes = early.minutesOfDay();
        assertEquals((24*60)+14, minutes);

        TramTime result = TramTime.fromMinutes(minutes);
        assertEquals(early, result);

        //TramTime next = early.plusMinutes(60);
        //minutes = next.minutesOfDay();
        //assertEquals((25*60)+14, minutes);

//        result = TramTime.fromMinutes(minutes);
//        assertEquals(next, result);
//
//        // some Stops (i.e. Ashton) in live data include hours=26 and hours=27
//        next = early.plusMinutes(120);
//        minutes = next.minutesOfDay();
//        assertEquals((26*60)+14, minutes);
//
//        result = TramTime.fromMinutes(minutes);
//        assertEquals(result, next);
    }

    @Test
    public void shouldSubstractMins() throws TramchesterException {
        TramTime timeA = TramTime.create(12,04);
        TramTime result = timeA.minusMinutes(30);
        assertEquals(11,result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    public void shouldAddMins() throws TramchesterException {
        TramTime timeA = TramTime.create(12,34);
        TramTime result = timeA.plusMinutes(30);
        assertEquals(13,result.getHourOfDay());
        assertEquals(4, result.getMinuteOfHour());
    }

    private void checkTimeFromMins(int minsOfDay, int hours, int minsOfHour) throws TramchesterException {
        TramTime one = TramTime.fromMinutes(minsOfDay);
        assertEquals(hours, one.getHourOfDay());
        assertEquals(minsOfHour, one.getMinuteOfHour());
        assertEquals(minsOfDay, one.minutesOfDay());
    }

    private void checkCorrectTimePresent(Optional<TramTime> resultA, int hours, int minutes) {
        assertTrue(resultA.isPresent());
        TramTime time = resultA.get();
        assertEquals(hours, time.getHourOfDay());
        assertEquals(minutes, time.getMinuteOfHour());
    }
}
