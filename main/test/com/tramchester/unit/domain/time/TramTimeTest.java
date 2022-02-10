package com.tramchester.unit.domain.time;

import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.time.TramTime.nextDay;
import static com.tramchester.domain.time.TramTime.of;
import static org.junit.jupiter.api.Assertions.*;

class TramTimeTest {

    @Test
    void shouldCreateTramTime() {
        TramTime timeA = of(11,23);
        assertEquals(11, timeA.getHourOfDay());
        assertEquals(23, timeA.getMinuteOfHour());
    }

    @Test
    void shouldHaveEquality() {
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute++) {
                TramTime tramTime = of(hour, minute);
                assertEquals(of(hour, minute), tramTime);
                assertNotEquals(of(23-hour, 59-minute), tramTime);
            }
        }
        assertNotEquals(of(11,42), nextDay(11,42));
        assertNotEquals(nextDay(11,42), of(11,42));
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
        checkCorrectTimePresent(TramTime.parse("00:35+24"), 0, 35, true);
        checkCorrectTimePresent(TramTime.parse("01:47+24"), 1, 47, true);
        checkCorrectTimePresent(TramTime.parse("02:42+24"), 2, 42, true);
    }

    @Test
    void shouldParseEmptyIfInvalid() {
        assertFalse(TramTime.parse("49:12").isValid());
        assertFalse(TramTime.parse("12:99").isValid());
    }

    @Test
    void shouldFormatCorrectly() {
        TramTime time = of(18,56);
        assertEquals("18:56",time.toPattern());
        assertEquals("18:56",time.serialize());

        TramTime nextDay = nextDay(11,42);
        assertEquals("11:42",nextDay.toPattern());
        assertEquals("11:42+24",nextDay.serialize());
    }

    @Test
    void shouldBeComparableDuringDaySameHour() {
        TramTime timeA = of(12, 4);
        TramTime timeB =  of(12, 3);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = of(12, 3);
        assertEquals(0, timeC.compareTo(timeB));
        assertEquals(0, timeB.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldBeComparableDuringDayDifferentHour() {
        TramTime timeA = of(13, 1);
        TramTime timeB =  of(12, 3);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldBeComparableAcrossMidnight() {
        TramTime timeA = nextDay(0,10);
        TramTime timeB =  of(23,10);

        assertTrue(timeA.compareTo(timeB)>0);
        assertTrue(timeB.compareTo(timeA)<0);

        TramTime timeC = nextDay(0,10);
        assertEquals(0, timeC.compareTo(timeA));
        assertEquals(0, timeA.compareTo(timeC));

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldImplementComparableOnSteams() {
        List<TramTime> list = Arrays.asList(of(10,12), of(8,15), of(9,57));

        List<TramTime> result = list.stream().sorted(TramTime.comparing(tramTime -> tramTime)).collect(Collectors.toList());
        assertEquals(of(8,15), result.get(0));
        assertEquals(of(9,57), result.get(1));
        assertEquals(of(10,12), result.get(2));
    }

    @Test
    void shouldOrderTramTimesCorrectlyOverMidnight() {
        TramTime timeA = nextDay(0,10);
        TramTime timeB =  of(23,10); // show first

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldOrderTramTimesNearMidnight() {
        TramTime timeA = of(23,47);
        TramTime timeB =  of(23,23); // show first

        SortedSet<TramTime> set = new TreeSet<>();
        set.add(timeA);
        set.add(timeB);

        assertEquals(timeB,set.first());
    }

    @Test
    void shouldCheckIfDepartsAfter() {
        TramTime timeA = of(LocalTime.of(12,15));
        TramTime timeB =  of(LocalTime.of(9,10));

        assertTrue(timeA.departsAfter(timeB));

        timeA = of(LocalTime.of(6,12));
        timeB = of(LocalTime.of(6,11));

        assertTrue(timeA.departsAfter(timeB));
        Assertions.assertFalse(timeB.departsAfter(timeA));
    }

    @Test
    void shouldCheckIfDepartsAfterDiffDays() {
        TramTime timeA = nextDay(0,10);
        TramTime timeB =  of(LocalTime.of(23,10));

        assertTrue(timeA.departsAfter(timeB));

        timeA = of(LocalTime.of(6,12));
        timeB = of(LocalTime.of(6,11));

        assertTrue(timeA.departsAfter(timeB));
        Assertions.assertFalse(timeB.departsAfter(timeA));
    }

    @Test
    void shouldHaveIsBefore() {
        assertTrue(of(11,33).isBefore(of(12, 0)));
        assertTrue(of(0,3).isBefore(of(0,30)));
        assertTrue(of(11,3).isBefore(nextDay(0,30)));

        Assertions.assertFalse(of(10,30).isBefore(of(10,30)));
        Assertions.assertFalse(of(10,30).isBefore(of(9,30)));
        Assertions.assertFalse(nextDay(0,30).isBefore(of(4,0))); // late night
    }

    @Test
    void shouldHaveAfter() {
        assertTrue(of(11,44).isAfter(of(11, 0)));
        assertTrue(nextDay(0,30).isAfter(of(23,30)));
        assertTrue(nextDay(0,30).isAfter(of(21,30)));
        assertTrue(of(0,44).isAfter(of(0,20)));
        assertTrue(of(2,44).isAfter(of(2,20)));

        Assertions.assertFalse(of(2,30).isAfter(of(23,30)));
        Assertions.assertFalse(of(4,30).isAfter(nextDay(0,30)));
    }

    @Test
    void shouldIfBetweenAccountingForMidnight() {
        TramTime morning = of(11,30);

        assertTrue(morning.between(of(9,0), of(13,0)));
        assertTrue(morning.between(of(11,30), of(13,0)));
        assertTrue(morning.between(of(10,30), of(11,30)));

        Assertions.assertFalse(morning.between(of(9,0), of(11,0)));
        Assertions.assertFalse(morning.between(of(12,0), of(13,0)));

        assertTrue(morning.between(of(5,0), nextDay(0,1)));
        assertTrue(morning.between(of(5,0), nextDay(0,0)));
        assertTrue(morning.between(of(5,0), nextDay(1,0)));

        TramTime earlyMorning = nextDay(0,20);
        assertTrue(earlyMorning.between(nextDay(0,1), nextDay(0,21)));
        assertTrue(earlyMorning.between(nextDay(0,0), nextDay(0,21)));
        assertTrue(earlyMorning.between(nextDay(0,1), nextDay(0,20)));
        assertTrue(earlyMorning.between(nextDay(0,0), nextDay(0,20)));
        assertTrue(earlyMorning.between(of(5,0), nextDay(1,20)));
        Assertions.assertFalse(earlyMorning.between(nextDay(3,0), nextDay(11,20)));
        Assertions.assertFalse(earlyMorning.between(of(23,0), nextDay(0,15)));
    }

    @Test
    void shouldHaveCorrectDifferenceIncludingTimesAcrossMidnight() {
        TramTime first = of(9,30);
        TramTime second = of(10,45);

        int result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(75, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(75, result);

        ////
        first = nextDay(0,5);
        second = of(23,15);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(50, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(50, result);

        ////
        first = nextDay(0,5);
        second = of(22,59);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(66, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(66, result);

        ////
        first = of(23,59);
        second = nextDay(1,10);

        result = TramTime.diffenceAsMinutes(first, second);
        assertEquals(71, result);

        result = TramTime.diffenceAsMinutes(second, first);
        assertEquals(71, result);
    }

    @Test
    void shouldAddMins() {
        TramTime ref = of(0,0);
        assertEquals(of(0,42), ref.plusMinutes(42));
        assertEquals(of(1,42), ref.plusMinutes(42+60));
        assertEquals(of(1,43), ref.plusMinutes(42+61));
        assertEquals(of(2,42), ref.plusMinutes(42+120));
        assertEquals(of(2,43), ref.plusMinutes(42+121));

        ref = of(23,10);
        assertEquals(of(23,52), ref.plusMinutes(42));
        assertEquals(nextDay(0,9), ref.plusMinutes(59));
        assertEquals(nextDay(0,52), ref.plusMinutes(42+60));
        assertEquals(nextDay(0,53), ref.plusMinutes(42+61));
        assertEquals(nextDay(1,52), ref.plusMinutes(42+120));
        assertEquals(nextDay(1,53), ref.plusMinutes(42+121));
    }

    @Test
    void shouldAddDuration() {
        TramTime ref = of(0,0);
        assertEquals(of(0,42), ref.plus(Duration.ofMinutes(42)));
        assertEquals(of(2,42), ref.plus(Duration.ofMinutes(42+120)));
        assertEquals(of(0,42), ref.plus(Duration.ofSeconds(42*60)));

        ref = of(23,10);
        assertEquals(of(23,52), ref.plus(Duration.ofMinutes(42)));
        assertEquals(nextDay(0,9), ref.plus(Duration.ofMinutes((59))));

    }

    @Test
    void shouldAddSubMinsNextDay() {
        TramTime ref = nextDay(11,42);
        assertEquals(nextDay(11,52), ref.plusMinutes(10));
        assertEquals(nextDay(11,32), ref.minusMinutes(10));
    }

    @Test
    void shouldSubstractMins() {
        TramTime reference = of(12, 4);
        TramTime result = reference.minusMinutes(30);

        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = of(12, 4);
        result = reference.minusMinutes(90);

        assertEquals(10, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    void shouldSubtractDuration() {
        TramTime reference = of(12, 4);

        TramTime result = reference.minus(Duration.ofMinutes(30));
        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        result = reference.minus(Duration.ofSeconds(60));
        assertEquals(12, result.getHourOfDay());
        assertEquals(3, result.getMinuteOfHour());

        result = reference.minus(Duration.ofSeconds(60*30));
        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    void shouldThrowIfAccuracyIsLostOnSubtraction() {
        TramTime reference = of(12, 4);

        assertThrows(RuntimeException.class, () -> reference.minus(Duration.ofSeconds(31)));
        assertThrows(RuntimeException.class, () -> reference.minus(Duration.ofSeconds(29)));
    }

    @Test
    void shouldThrowIfAccuracyIsLostOnAddition() {
        TramTime reference = of(12, 4);

        assertThrows(RuntimeException.class, () -> reference.plus(Duration.ofSeconds(31)));
        assertThrows(RuntimeException.class, () -> reference.plus(Duration.ofSeconds(29)));
    }

    @Test
    void shouldNotThrowIfSafeConversationIsUsed() {
        LocalTime now = LocalTime.now();
        final LocalTime time = now.getSecond()==0 ? now.plusSeconds(1) : now;

        TramTime.ofHourMins(time);
    }

    @Test
    void shouldFindTimesWithinInterval() {
        TramTime reference = of(15, 24);
        assertTrue(of(15, 15).withinInterval(10, reference));
        assertTrue(of(15, 15).withinInterval(9, reference));
        assertFalse(of(15, 15).withinInterval(8, reference));

        assertTrue(of(14, 25).withinInterval(60, reference));
    }

    @Test
    void shouldFindTimesWithinIntervalOrMidight() {
        TramTime reference = of(0, 45);
        assertTrue(of(0, 15).withinInterval(30, reference));
        assertTrue(of(0, 15).withinInterval(90, reference));
        assertFalse(of(0, 5).withinInterval(30, reference));
    }

    @Test
    void shouldFindTimesWithinIntervalCrossingDayBoundary() {
        TramTime reference = nextDay(1,30);
        assertTrue(nextDay(1, 15).withinInterval(30, reference));
        assertTrue(of(23, 45).withinInterval(120, reference));
        assertFalse(of(22, 45).withinInterval(120, reference));
    }

    @Test
    void shouldSubtractWithResultInPreviousDay() {
        TramTime reference = nextDay(0, 4);

        TramTime result = reference.minusMinutes(30);

        assertFalse(result.isNextDay());
        assertEquals(23, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());

        reference = nextDay(0, 4);
        result = reference.minusMinutes(90);

        assertFalse(result.isNextDay());
        assertEquals(22, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    void shouldSubstractMinsViaLocalTime() {
        LocalTime reference = LocalTime.of(12, 4);
        TramTime result = of(reference.minusMinutes(30));

        assertEquals(11, result.getHourOfDay());
        assertEquals(34, result.getMinuteOfHour());
    }

    @Test
    void shouldGetCorrectDaySameDay() {
        LocalDate beginDate = TestEnv.testDay();

        TramTime sameDay = of(11,42);

        LocalDateTime result = sameDay.toDate(beginDate);
        assertEquals(sameDay.asLocalTime(), result.toLocalTime());
        assertEquals(beginDate, result.toLocalDate());
    }

    @Test
    void shouldGetCorrectDayNextDay() {
        LocalDate beginDate = TestEnv.testDay();

        TramTime sameDay = nextDay(11,42);

        LocalDateTime result = sameDay.toDate(beginDate);
        assertEquals(sameDay.asLocalTime(), result.toLocalTime());
        assertEquals(beginDate.plusDays(1), result.toLocalDate());
    }

    private void checkCorrectTimePresent(TramTime tramTime, int hours, int minutes, boolean nextDay) {
        assertTrue(tramTime.isValid());
        assertEquals(hours, tramTime.getHourOfDay());
        assertEquals(minutes, tramTime.getMinuteOfHour());
        assertEquals(nextDay, tramTime.isNextDay());
    }
}
