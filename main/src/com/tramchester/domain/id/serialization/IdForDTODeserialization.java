package com.tramchester.domain.id.serialization;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tramchester.domain.id.IdForDTO;

import java.io.IOException;

public class IdForDTODeserialization extends StdDeserializer<IdForDTO> {

    protected IdForDTODeserialization(Class<?> vc) {
        super(vc);
    }

    protected IdForDTODeserialization() {
        this(null);
    }

    @Override
    public IdForDTO deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        String id = node.asText();
        return new IdForDTO(id);
    }
}
