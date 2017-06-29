package com.tramchester.integration.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.LocalTime;

import java.io.IOException;

public class TimeJsonSerializer extends JsonSerializer<LocalTime> {
    public static String pattern = "HH:mm";

    @Override
    public void serialize(LocalTime time, JsonGenerator gen,
                          SerializerProvider arg2)
            throws IOException {

        gen.writeString(time.toString(pattern));
    }
}
