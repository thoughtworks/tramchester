package com.tramchester.dataimport.parsers;

import com.googlecode.jcsv.reader.CSVEntryParser;
import com.tramchester.dataimport.data.StopTimeData;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.lang.String.format;


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

        String hour = split[0];
        if (hour.equals("24") || hour.equals("25")) {
            hour = "00";
        }
        String minutes = split[1];
        String string = "not set";
        try {
            if (split.length==3) {
                string = format("%s:%s:%s",Integer.parseInt(hour), Integer.parseInt(minutes), Integer.parseInt(split[2]));
            } else {
                string = format("%s:%s",Integer.parseInt(hour), Integer.parseInt(minutes));
            }
            LocalTime parsedTime = LocalTime.parse(string);
            return Optional.of(parsedTime);
        }
        catch (IllegalFieldValueException exception) {
            logger.error("Caught Expection during creation of date. Unable to parse "+string, exception);
            // can't catch and convert here due to the inherited interface
        }
        return Optional.empty();
    }
}