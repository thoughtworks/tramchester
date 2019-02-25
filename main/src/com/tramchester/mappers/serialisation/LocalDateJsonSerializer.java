package com.tramchester.mappers.serialisation;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateJsonSerializer extends JsonSerializer<LocalDate> {

    // was YYYY-MM-dd
    public static String pattern = "yyyy-MM-dd";

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.format(DateTimeFormatter.ofPattern(pattern)));
    }
}
