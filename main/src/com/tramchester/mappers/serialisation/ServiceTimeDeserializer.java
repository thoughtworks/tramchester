package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;

import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;

import static java.lang.String.format;

public class ServiceTimeDeserializer extends JsonDeserializer<ServiceTime> {
    @Override
    public ServiceTime deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException, JsonProcessingException {
       throw new RuntimeException("WIP");
    }
}
