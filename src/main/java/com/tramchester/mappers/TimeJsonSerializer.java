package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimeJsonSerializer extends JsonSerializer<LocalTime> {
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void serialize(LocalTime time, JsonGenerator gen,
                          SerializerProvider arg2)
            throws IOException {

        gen.writeString(time.format(formatter));
    }
}
