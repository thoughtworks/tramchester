package com.tramchester.unit.mappers;

import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class LocalDateJsonDeserializerTest {

    @Test
    void shouldParseDateStringCorrectly() {
        String input = "2018-10-23";

        LocalDateJsonDeserializer deserializer = new LocalDateJsonDeserializer();
        LocalDate result = deserializer.getLocalDate(input);

        Assertions.assertEquals(2018, result.getYear());
        Assertions.assertEquals(10, result.getMonthValue());
        Assertions.assertEquals(23, result.getDayOfMonth());
    }
}
