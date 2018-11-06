package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.tramchester.config.TramchesterConfig;

import java.io.IOException;
import java.time.LocalDateTime;

public class LocalDateTimeJsonSerializeAsMillis extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime time, JsonGenerator gen, SerializerProvider arg2) throws IOException {
        long millis = time.atZone(TramchesterConfig.TimeZone).toInstant().toEpochMilli();
        gen.writeNumber(millis);
    }
}
