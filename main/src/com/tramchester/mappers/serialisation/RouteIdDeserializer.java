package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;

import java.io.IOException;

@Deprecated
public class RouteIdDeserializer extends StdDeserializer<IdFor<Route>> {

    protected RouteIdDeserializer() {
        this(null);
    }

    protected RouteIdDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public IdFor<Route> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        return Route.createId(node.asText());
    }
}
