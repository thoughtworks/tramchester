package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;

import java.io.IOException;

@Deprecated
public class RouteIdSerializer extends StdSerializer<IdFor<Route>> {

    protected RouteIdSerializer() {
        this(null);
    }

    protected RouteIdSerializer(Class<IdFor<Route>> t) {
        super(t);
    }

    @Override
    public void serialize(IdFor<Route> id, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(id.getGraphId());
    }
}
