package com.tramchester.unit.domain.presentation.DTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tramchester.domain.id.IdForDTO;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.presentation.DTO.JourneyQueryDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JourneyQueryDTOTest {

    @Test
    void shouldSerializedDeserialize() throws JsonProcessingException {
        JourneyQueryDTO dto = new JourneyQueryDTO(LocalDate.of(2022, 11, 15),
                LocalTime.of(13,56), LocationType.Station, new IdForDTO("startId"),
                LocationType.Station, new IdForDTO("destId"), false, 3);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String txt = mapper.writeValueAsString(dto);

        JourneyQueryDTO result = mapper.readValue(txt, JourneyQueryDTO.class);

        assertEquals(dto.getDate(), result.getDate());
        assertEquals(dto.getTime(), result.getTime());
        assertEquals(dto.getDestType(), result.getDestType());
        assertEquals(dto.getDestId(), result.getDestId());
        assertEquals(dto.getStartType(), result.getStartType());
        assertEquals(dto.getStartId(), result.getStartId());
        assertEquals(dto.getMaxChanges(), result.getMaxChanges());
        assertEquals(dto.getModes(), result.getModes());


    }
}
