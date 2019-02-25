package com.tramchester.mappers.serialisation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;

public class LocalDateJsonDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);

        return getLocalDate(node.asText());
    }

    public LocalDate getLocalDate(String text) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern(LocalDateJsonSerializer.pattern);
        TemporalAccessor result = format.parse(text);
        // TODO must be better way to do this...
        TemporalField temporalField = WeekFields.ISO.weekBasedYear();
        int year = result.get(temporalField);
        int month = result.get(ChronoField.MONTH_OF_YEAR);
        int dayOfMonth = result.get(ChronoField.DAY_OF_MONTH);
        return LocalDate.of(year, month, dayOfMonth);
    }
}
