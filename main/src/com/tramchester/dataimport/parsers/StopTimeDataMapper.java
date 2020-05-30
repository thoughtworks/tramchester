package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.time.TramTime;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

public class StopTimeDataMapper extends CSVEntryMapper<StopTimeData> {
    private static final Logger logger = LoggerFactory.getLogger(StopTimeDataMapper.class);
    private final Set<String> tripIds;
    private final boolean includeAll;

    public StopTimeDataMapper(Set<String> tripIds) {
        this.tripIds = tripIds;
        includeAll = (tripIds.size()==0);
    }

    public StopTimeData parseEntry(CSVRecord data) {
        String tripId = getTripId(data);
        Optional<TramTime> arrivalTime;
        Optional<TramTime> departureTime;

        String fieldOne = data.get(1);
        arrivalTime = parseTimeField(fieldOne, tripId);

        String fieldTwo = data.get(2);
        departureTime = parseTimeField(fieldTwo, tripId);

        String stopId = data.get(3);

        String stopSequence = data.get(4);
        String pickupType = data.get(5);
        String dropOffType = data.get(6);

        if (arrivalTime.isEmpty() || departureTime.isEmpty()) {
            logger.error("Failed to parse arrival time from fields " + data);
            throw new RuntimeException("Unable to parse time for " + data);
        }

        return new StopTimeData(tripId, arrivalTime.get(), departureTime.get(), stopId, stopSequence, pickupType, dropOffType);
    }

    private String getTripId(CSVRecord data) {
        return data.get(0);
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

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return tripIds.contains(getTripId(data));
    }

}