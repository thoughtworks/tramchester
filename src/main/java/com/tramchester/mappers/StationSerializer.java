package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.tramchester.domain.Station;

import java.io.IOException;


public class StationSerializer extends JsonSerializer<Station> {

    // TODO do this properly
    @Override
    public void serialize(Station value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value.getId());
    }
}
