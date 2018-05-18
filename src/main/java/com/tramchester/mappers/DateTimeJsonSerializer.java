package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.DateTime;

import java.io.IOException;

public class DateTimeJsonSerializer extends JsonSerializer<DateTime> {

    @Override
    public void serialize(DateTime time, JsonGenerator gen,
                          SerializerProvider arg2)
            throws IOException {

        gen.writeString(time.toString(DateTimeJsonDeserializer.pattern));
    }
}
