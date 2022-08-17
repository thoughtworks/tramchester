package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.TramServiceDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TramServiceDateTest {

    @Test
    void shouldInitiateWithDate() {
        LocalDate date = LocalDate.of(2014, 10, 25);
        TramServiceDate tramServiceDate = new TramServiceDate(date);

        assertThat(tramServiceDate.getDate()).isEqualTo(date);
        assertThat(tramServiceDate.getStringDate()).isEqualTo("20141025");
    }

    @Test
    void shouldCheckBetween() {
        LocalDate aDate = LocalDate.of(2016, 6, 13);
        TramServiceDate tramServiceDate = new TramServiceDate(aDate);

        Assertions.assertTrue(tramServiceDate.within(aDate, aDate.plusDays(1)));
        Assertions.assertTrue(tramServiceDate.within(aDate.minusDays(1), aDate));
        Assertions.assertTrue(tramServiceDate.within(aDate, aDate));
        Assertions.assertTrue(tramServiceDate.within(aDate.minusDays(1), aDate.plusDays(1)));
        Assertions.assertFalse(tramServiceDate.within(aDate.minusDays(2), aDate.minusDays(1)));
        Assertions.assertFalse(tramServiceDate.within(aDate.plusDays(1), aDate.plusDays(2)));

    }
}