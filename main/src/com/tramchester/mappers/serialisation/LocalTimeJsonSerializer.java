package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalTime;

public class LocalTimeJsonSerializer extends JsonSerializer<LocalTime> {

    @Override
    public void serialize(LocalTime time, JsonGenerator gen, SerializerProvider arg2) throws IOException {
        gen.writeString(time.format(LocalTimeJsonDeserializer.LOCAL_TIME));
    }
}
