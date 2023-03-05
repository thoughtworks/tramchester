package com.tramchester.domain.id.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.id.IdForDTO;

import java.io.IOException;

public class IdForDTOSerialization extends StdSerializer<IdForDTO> {

    protected IdForDTOSerialization(Class<IdForDTO> t) {
        super(t);
    }

    protected IdForDTOSerialization() {
        this(null);
    }
    @Override
    public void serialize(IdForDTO value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getActualId());
    }
}
