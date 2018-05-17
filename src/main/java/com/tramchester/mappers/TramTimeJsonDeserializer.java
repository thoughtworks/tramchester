package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tramchester.domain.TramTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.Optional;

public class TramTimeJsonDeserializer extends JsonDeserializer<TramTime> {
    @Override
    public TramTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        Optional<TramTime> result = TramTime.parse(node.asText());
        if (!result.isPresent()) {
            throw new IOException("Failed to parse " + node.asText());
        }
        return result.get();
    }
}
