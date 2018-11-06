package com.tramchester.unit.mappers;

import com.tramchester.mappers.serialisation.LocalDateJsonDeserializer;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class LocalDateJsonDeserializerTest {

    @Test
    public void shouldParseDateStringCorrectly() {
        String input = "2018-10-23";

        LocalDateJsonDeserializer deserializer = new LocalDateJsonDeserializer();
        LocalDate result = deserializer.getLocalDate(input);

        assertEquals(2018, result.getYear());
        assertEquals(10, result.getMonthValue());
        assertEquals(23, result.getDayOfMonth());
    }
}
