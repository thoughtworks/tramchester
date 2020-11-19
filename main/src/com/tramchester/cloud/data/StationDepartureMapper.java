package com.tramchester.cloud.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.tramchester.domain.presentation.DTO.StationDepartureInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class StationDepartureMapper {
    private static final Logger logger = LoggerFactory.getLogger(StationDepartureMapper.class);

    private final ObjectMapper mapper;

    public StationDepartureMapper() {
        mapper = new ObjectMapper();
    }

    public String map(List<StationDepartureInfoDTO> departures) throws JsonProcessingException {
        String json = mapper.writeValueAsString(departures);
        logger.debug("Created json: " + json);
        return json;
    }

    public List<StationDepartureInfoDTO> parse(String json) {
        ObjectReader reader = mapper.readerForListOf(StationDepartureInfoDTO.class);
        logger.debug("Parse json: " + json);
        try {
            return reader.readValue(json);
        } catch (JsonProcessingException exception) {
            logger.error("Unable to parse json "+ json, exception);
            return Collections.emptyList();
        }
    }
}
