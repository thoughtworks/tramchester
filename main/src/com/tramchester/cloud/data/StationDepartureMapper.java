package com.tramchester.cloud.data;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.livedata.domain.DTO.StationDepartureInfoDTO;
import com.tramchester.livedata.domain.DTO.archived.ArchivedStationDepartureInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

@LazySingleton
public class StationDepartureMapper {
    private static final Logger logger = LoggerFactory.getLogger(StationDepartureMapper.class);

    private final ObjectMapper mapper;

    @Inject
    public StationDepartureMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String map(List<StationDepartureInfoDTO> departures) throws JsonProcessingException {
        String json = mapper.writeValueAsString(departures);
        logger.debug("Created json: " + json);
        return json;
    }

    public List<ArchivedStationDepartureInfoDTO> parse(String json) {
        ObjectReader reader = mapper.readerForListOf(ArchivedStationDepartureInfoDTO.class);
        if (logger.isDebugEnabled()) {
            logger.debug("Parsing json with length " + json.length());
            logger.debug("Parse json: " + json);
        }
        try {
            return reader.readValue(json);
        } catch (JsonProcessingException exception) {
            /// full text into debug log
            logger.debug("Unable to parse json "+ json, exception);

            JsonLocation location = exception.getLocation();
            if (location==null) {
                logger.warn("Unable to parse json and no location information provided ", exception);
            } else {
                int offset = (int) location.getCharOffset();
                char badChar = json.charAt(offset);
                String context = json.substring(Math.max(0,offset-10), Math.min(json.length()-1,offset+10));
                logger.warn(format("Unable to process json at offset %s, context '%s' and char was '%s' (char %s)",
                        offset, context, badChar, (int)badChar));
                logger.warn("Json:" + json + " Exception:" + exception);
            }
            return Collections.emptyList();
        }
    }
}
