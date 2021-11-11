package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;

import java.io.IOException;

public class RouteIdDeserializer extends StdDeserializer<IdFor<RouteReadOnly>> {

    protected RouteIdDeserializer() {
        this(null);
    }

    protected RouteIdDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public IdFor<RouteReadOnly> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        return StringIdFor.createId(node.asText());
    }
}
