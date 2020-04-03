package com.tramchester.unit.domain;

import com.tramchester.testSupport.TestEnv;
import org.junit.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static junit.framework.TestCase.assertEquals;


public class GenerateExampleDatesTest {

    @Test
    public void shouldGenerateCorrectDaysForTests() {
        LocalDate generated = TestEnv.nextTuesday(0);

        assertEquals(DayOfWeek.TUESDAY, generated.getDayOfWeek());
    }
}
