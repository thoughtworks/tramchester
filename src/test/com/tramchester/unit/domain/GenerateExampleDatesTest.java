package com.tramchester.unit.domain;

import com.tramchester.TestConfig;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;


public class GenerateExampleDatesTest {

    @Test
    public void shouldGenerateCorrectDaysForTests() {
        LocalDate generated = TestConfig.nextTuesday(0);

        assertEquals(DayOfWeek.TUESDAY, generated.getDayOfWeek());
    }
}
