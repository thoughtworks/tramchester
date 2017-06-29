package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopTimeData;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class StopTimeDataParser implements CSVEntryParser<StopTimeData> {
    private static final Logger logger = LoggerFactory.getLogger(StopTimeDataParser.class);

    public StopTimeData parseEntry(String... data) {
        String tripId = data[0];
        Optional<LocalTime> arrivalTime = Optional.empty();
        Optional<LocalTime> departureTime = Optional.empty();

        String fieldOne = data[1];
        if (fieldOne.contains(":")) {
            arrivalTime = getDateTime(fieldOne);
        }

        if (data[2].contains(":")) {
            departureTime = getDateTime(data[2]);
        }

        String stopId = data[3];

        String stopSequence = data[4];
        String pickupType = data[5];
        String dropOffType = data[6];

        return new StopTimeData(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
    }


    private Optional<LocalTime> getDateTime(String time) {
        String[] split = time.split(":");

        Integer hour = Integer.parseInt(split[0]);
        if (hour==24 || hour==25) {
            hour = 0;
        }
        Integer minutes = Integer.parseInt(split[1]);
        try {
            if (split.length==3) {
                return Optional.of(new LocalTime(hour,minutes,
                        Integer.parseInt(split[2])));
            } else {
                return Optional.of(new LocalTime(hour,minutes));
            }
        }
        catch (IllegalFieldValueException exception) {
            logger.error("Caught Expection during creation of date. Unable to parse "+time, exception);
            // can't catch and convert here due to the inherited interface
        }
        return Optional.empty();
    }
}