package com.tramchester.mappers;


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.LocalDate;

import java.io.IOException;

public class LocalDateJsonSerializer extends JsonSerializer<LocalDate> {

    public static String pattern = "YYYY-MM-dd";

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.toString(pattern));
    }
}
