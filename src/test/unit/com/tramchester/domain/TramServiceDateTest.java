package com.tramchester.domain;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TramServiceDateTest {

    @Test
    public void shouldInitiateWithDate() throws Exception {
        LocalDate date = new LocalDate(2014, 10, 25);
        TramServiceDate tramServiceDate = new TramServiceDate(date);

        assertThat(tramServiceDate.getDate()).isEqualTo(date);
        assertThat(tramServiceDate.getStringDate()).isEqualTo("20141025");
    }

    @Test
    public void shouldInitiateWithStringDate() throws Exception {
        LocalDate date = new LocalDate(2014, 10, 25);
        TramServiceDate tramServiceDate = new TramServiceDate("20141025");

        assertThat(tramServiceDate.getDate()).isEqualTo(date);
        assertThat(tramServiceDate.getStringDate()).isEqualTo("20141025");
    }
}