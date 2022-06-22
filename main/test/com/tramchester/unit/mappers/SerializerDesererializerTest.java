package com.tramchester.unit.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.tramchester.domain.time.TramTime;
import com.tramchester.mappers.serialisation.*;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerializerDesererializerTest extends EasyMockSupport {

    private final TramTime tramTime = TramTime.of(11, 42);
    private final String tramTimeJson = "{\"value\":\"11:42\"}";

    private final TramTime tramTimeNextDay = TramTime.nextDay(11, 42);
    private final String tramTimeNextDayJson = "{\"value\":\"11:42+24\"}";

    private final LocalDate localDate = LocalDate.of(2018, 11, 23);
    private final String localDateJson = "{\"value\":\"2018-11-23\"}";

    private final LocalDateTime localDateTime = LocalDateTime.of(2020, 11, 23, 14, 43, 55);
    private final String localDateTimeJson = "{\"value\":\"2020-11-23T14:43:55\"}";
    private final String localDateTimeAsMillisJson = "{\"value\":1606142635000}";

    private ObjectMapper mapper;

    @BeforeEach
    void beforeEachTestRuns() {
        mapper = JsonMapper.builder().addModule(new AfterburnerModule()).build();
    }

    @Test
    void shouldDeserializeLocalDate() throws JsonProcessingException {
        ExampleForLocalDate result = mapper.readValue(localDateJson, ExampleForLocalDate.class);
        assertEquals(localDate, result.value);
    }

    @Test
    void shouldSerializeLocalDateWithMapper() throws IOException {
        assertEquals(localDateJson, getSerialised(new ExampleForLocalDate(localDate)));
    }

    @Test
    void shouldDeserialiseTramTime() throws JsonProcessingException {
        ExampleForTramTime result = mapper.readValue(tramTimeJson, ExampleForTramTime.class);
        assertEquals(tramTime, result.value);
    }

    @Test
    void shouldSerializeTramTime() throws IOException {
        assertEquals(tramTimeJson, getSerialised(new ExampleForTramTime(tramTime)));
    }

    @Test
    void shouldDeserialiseTramTimeNextDay() throws JsonProcessingException {
        ExampleForTramTime result = mapper.readValue(tramTimeNextDayJson, ExampleForTramTime.class);
        assertEquals(tramTimeNextDay, result.value);
    }

    @Test
    void shouldSerializeTramTimeNextDay() throws IOException {
        assertEquals(tramTimeNextDayJson, getSerialised(new ExampleForTramTime(tramTimeNextDay)));
    }

    @Test
    void shouldSerializeLocalDateTime() throws IOException {
        assertEquals(localDateTimeJson, getSerialised(new ExampleForLocalDateTime(localDateTime)));
    }

    @Test
    void shouldDeserialiseLocalDateTime() throws JsonProcessingException {
        ExampleForLocalDateTime result = mapper.readValue(localDateTimeJson, ExampleForLocalDateTime.class);
        assertEquals(localDateTime, result.value);
    }

    @Test
    void shouldSerializeLocalDateTimeAsMillis() throws IOException {
        assertEquals(localDateTimeAsMillisJson, getSerialised(new ExampleForLocalDateTimeAsMillis(localDateTime)));
    }

    @Test
    void shouldDeserialiseLocalDateTimeAsMillis() throws JsonProcessingException {
        ExampleForLocalDateTimeAsMillis result = mapper.readValue(localDateTimeAsMillisJson, ExampleForLocalDateTimeAsMillis.class);
        assertEquals(localDateTime, result.value);
    }

    private String getSerialised(Object example) throws IOException {
        StringWriter output = new StringWriter();
        mapper.writeValue(output, example);
        output.flush();
        return output.getBuffer().toString();
    }

    private static class ExampleForTramTime {

        @JsonDeserialize(using=TramTimeJsonDeserializer.class)
        @JsonSerialize(using=TramTimeJsonSerializer.class)
        public TramTime value;

        public ExampleForTramTime() {
            // deserialise
        }

        public ExampleForTramTime(TramTime value) {
            this.value = value;
        }
    }

    private static class ExampleForLocalDate {
        @JsonDeserialize(using=LocalDateJsonDeserializer.class)
        @JsonSerialize(using=LocalDateJsonSerializer.class)
        public LocalDate value;

        public ExampleForLocalDate() {
            // deserialise
        }

        public ExampleForLocalDate(LocalDate value) {
            this.value = value;
        }
    }

    private static class ExampleForLocalDateTime {
        @JsonDeserialize(using= LocalDateTimeJsonDeserializer.class)
        @JsonSerialize(using=LocalDateTimeJsonSerializer.class)
        public LocalDateTime value;

        public ExampleForLocalDateTime() {
            // deserialise
        }

        public ExampleForLocalDateTime(LocalDateTime value) {
            this.value = value;
        }
    }

    private static class ExampleForLocalDateTimeAsMillis {
        @JsonDeserialize(using= LocalDateTimeJsonDeserializerAsMillis.class)
        @JsonSerialize(using=LocalDateTimeJsonSerializeAsMillis.class)
        public LocalDateTime value;

        public ExampleForLocalDateTimeAsMillis() {
            // deserialise
        }

        public ExampleForLocalDateTimeAsMillis(LocalDateTime value) {
            this.value = value;
        }
    }
}
