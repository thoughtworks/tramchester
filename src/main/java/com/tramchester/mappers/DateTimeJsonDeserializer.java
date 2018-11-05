package com.tramchester.mappers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeJsonDeserializer extends JsonDeserializer<LocalDateTime> {
    public static String pattern = "HH:mm"; // TODO shouldn't this include full date?

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        DateTimeFormatter format = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(node.asText(), format);
    }
}
