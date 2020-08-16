package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tramchester.config.TramchesterConfig;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

public class LocalDateTimeJsonDeserializerAsMillis extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        long millis = Long.parseLong(node.asText());
        return Instant.ofEpochMilli(millis).atZone(TramchesterConfig.TimeZone).toLocalDateTime();
    }
}
