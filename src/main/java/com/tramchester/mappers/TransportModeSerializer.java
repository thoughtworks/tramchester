package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.tramchester.domain.TransportMode;

import java.io.IOException;

public class TransportModeSerializer extends JsonSerializer<TransportMode> {
    @Override
    public void serialize(TransportMode value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.name());
    }
}
