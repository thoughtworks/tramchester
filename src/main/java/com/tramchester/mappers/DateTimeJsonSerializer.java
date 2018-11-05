package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.joda.time.DateTime;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeJsonSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime time, JsonGenerator gen,
                          SerializerProvider arg2)
            throws IOException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimeJsonDeserializer.pattern);
        gen.writeString(time.format(formatter));
        //gen.writeString(time.toString(DateTimeJsonDeserializer.pattern));
    }
}
