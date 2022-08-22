package com.tramchester.unit.domain.dates;

import com.tramchester.domain.dates.TramDate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TramDateTest {

    @Test
    void shouldHaveSameEpochDay() {
        for (int i = 1; i < 30; i++) {
            LocalDate date = LocalDate.of(2022, 7, i);
            TramDate tramDate = TramDate.of(2022, 7, i);

            assertEquals(date.toEpochDay(), tramDate.toEpochDay());
            assertEquals(date.getDayOfWeek(), tramDate.getDayOfWeek());
        }

        TramDate tramDate = TramDate.of(2022, 7, 1);
        for (int i = 1; i < 30; i++) {
            LocalDate date = LocalDate.of(2022, 7, i);

            assertEquals(date.toEpochDay(), tramDate.toEpochDay());
            assertEquals(date.getDayOfWeek(), tramDate.getDayOfWeek());

            tramDate = tramDate.plusDays(1);
        }
    }
}
