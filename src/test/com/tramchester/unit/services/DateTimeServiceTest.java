package com.tramchester.services;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DateTimeServiceTest {

    private DateTimeService dateTimeService = new DateTimeService();;

    @Test
    public void shouldCalculateMinutesFromMidnight() throws Exception {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight("09:34:00");

        assertThat(minutesFromMidnight).isEqualTo(574);
    }

    @Test
    public void shouldCalculateMinutesFromMidnightFor12AM() throws Exception {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight("00:12:00");

        assertThat(minutesFromMidnight).isEqualTo(1452);
    }

    @Test
    public void shouldCalculateMinutesFromMidnightFor1AM() throws Exception {
        int minutesFromMidnight = dateTimeService.getMinutesFromMidnight("01:32:00");

        assertThat(minutesFromMidnight).isEqualTo(1532);
    }
}