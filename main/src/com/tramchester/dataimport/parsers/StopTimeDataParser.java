package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.lang.String.format;


public class StopTimeDataParser implements CSVEntryParser<StopTimeData> {
    private static final Logger logger = LoggerFactory.getLogger(StopTimeDataParser.class);

    public StopTimeData parseEntry(String... data) {
        String tripId = data[0];
        Optional<TramTime> arrivalTime;
        Optional<TramTime> departureTime;

        String fieldOne = data[1];
        arrivalTime = parseTimeField(fieldOne, tripId);

        String fieldTwo = data[2];
        departureTime = parseTimeField(fieldTwo, tripId);

        String stopId = data[3];

        String stopSequence = data[4];
        String pickupType = data[5];
        String dropOffType = data[6];

        if (!arrivalTime.isPresent()) {
            logger.error("Failed to parse arrival time from fields", data);
        }
        if (!departureTime.isPresent()) {
            logger.error("Failed to parse arrival time from fields", data);
        }

        return new StopTimeData(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
    }

    private Optional<TramTime> parseTimeField(String fieldOne, String tripId) {
        Optional<TramTime> time = Optional.empty();
        if (fieldOne.contains(":")) {
            time = TramTime.parse(fieldOne);
        }
        if (!time.isPresent()) {
            logger.error(format("Failed to parse time '%s' for tripId '%s'",fieldOne,tripId));
        }
        return time;
    }


}