package com.tramchester.unit.domain;

import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;


class GenerateExampleDatesTest {

    @Test
    void shouldGenerateCorrectDaysForTests() {
        LocalDate generated = TestEnv.nextTuesday(0);

        Assertions.assertEquals(DayOfWeek.TUESDAY, generated.getDayOfWeek());
    }
}
