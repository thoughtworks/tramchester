package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.IdFor;

import java.io.IOException;

public class RouteIdSerializer extends StdSerializer<IdFor<RouteReadOnly>> {

    protected RouteIdSerializer() {
        this(null);
    }

    protected RouteIdSerializer(Class<IdFor<RouteReadOnly>> t) {
        super(t);
    }

    @Override
    public void serialize(IdFor<RouteReadOnly> id, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(id.getGraphId());
    }
}
