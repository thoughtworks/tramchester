package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.*;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("Performance Testing Only")
public class AggregateServiceCalendarPerfTest {

    private AggregateServiceCalendar aggregateServiceCalendar;
    private ThreadLocalRandom random;

    final int periodOfDays = 400;
    final int calendarNumber = 5000;
    private TramDate startDate;
    private List<ServiceCalendar> calendarList;

    @BeforeEach
    void setUp() {

        final int maxRangeSize = 365;

        random = ThreadLocalRandom.current();

        calendarList = new ArrayList<>(calendarNumber);

        startDate = TramDate.of(2014, 10, 5);

        for (int i = 0; i <calendarNumber; i++) {
            int offset = random.nextInt(0, periodOfDays);
            int daysForRange = random.nextInt(1, maxRangeSize);
            TramDate date = startDate.plusDays(offset);
            DateRange range = DateRange.of(date, date.plusDays(daysForRange));
            MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(range, getDaysOfWeek(random));

            int numberToAdd = random.nextInt(0, 10);
            for (int j = 0; j < numberToAdd; j++) {
                int addition = random.nextInt(0, daysForRange);
                serviceCalendar.includeExtraDate(date.plusDays(addition));
            }

            int numberToRemove = random.nextInt(0, 10);
            for (int j = 0; j < numberToRemove; j++) {
                int removed = random.nextInt(0, daysForRange);
                serviceCalendar.excludeDate(date.plusDays(removed));

            }
            calendarList.add(serviceCalendar);
        }

        aggregateServiceCalendar = new AggregateServiceCalendar(calendarList);
    }

    @RepeatedTest(150)
    void shouldExerciseAnyOverlapCalendarToCalendar() {
        final int first = random.nextInt(0, calendarNumber);
        int second = first;
        while (first==second) {
            second = random.nextInt(0, calendarNumber);
        }

        ServiceCalendar calendarA = calendarList.get(first);
        ServiceCalendar calendarB = calendarList.get(second);

        for (int i = 0; i < 1000000; i++) {
            calendarA.anyDateOverlaps(calendarB);
        }
    }

    @RepeatedTest(1500)
    void shouldExerciseAnyOverlapAggregateToAggregate() {
        AggregateServiceCalendar other = new AggregateServiceCalendar(calendarList);

        for (int i = 0; i < 1000000; i++) {
            aggregateServiceCalendar.anyDateOverlaps(other);
        }
    }

    @RepeatedTest(150)
    void shouldExerciseOperatesON() {

        int testSize = 10000000;
        for (int i = 0; i < testSize; i++) {
            int offset = random.nextInt(0, periodOfDays);
            aggregateServiceCalendar.operatesOn(startDate.plusDays(offset));
        }
    }

    private EnumSet<DayOfWeek> getDaysOfWeek(ThreadLocalRandom random) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        int numberToAdd = random.nextInt(0,6);
        for (int i = 0; i < numberToAdd; i++) {
            int day = random.nextInt(1, 7);
            days.add(DayOfWeek.of(day));
        }
        return days;
    }
}
