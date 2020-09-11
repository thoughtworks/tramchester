package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.tramchester.domain.time.TramTime;

import java.io.IOException;

public class TramTimeJsonSerializer extends JsonSerializer<TramTime> {

    @Override
    public void serialize(TramTime time, JsonGenerator gen, SerializerProvider serializerProvider)
            throws IOException {

        gen.writeString(time.serialize());
    }
}
