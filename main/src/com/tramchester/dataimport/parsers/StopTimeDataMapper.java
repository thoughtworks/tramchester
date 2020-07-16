package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.domain.time.TramTime;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

public class StopTimeDataMapper extends CSVEntryMapper<StopTimeData> {
    private static final Logger logger = LoggerFactory.getLogger(StopTimeDataMapper.class);
    private final Set<String> tripIds;
    private final boolean includeAll;
    private int indexOfId = -1;
    private int indexOfArrival = -1;
    private int indexOfDepart = -1;
    private int indexOfStopId = -1;
    private int indexOfStopSeq = -1;
    private int indexOfPickup = -1;
    private int indexOfDropOff = -1;

    private enum Columns implements ColumnDefination {
        trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type
    }

    public StopTimeDataMapper(Set<String> tripIds) {
        this.tripIds = tripIds;
        includeAll = (tripIds.size()==0);
    }

    @Override
    protected ColumnDefination[] getColumns() {
        return Columns.values();
    }

    @Override
    protected void initColumnIndex(List<String> headers) {
        indexOfId = findIndexOf(headers, Columns.trip_id);
        indexOfArrival = findIndexOf(headers, Columns.arrival_time);
        indexOfDepart = findIndexOf(headers, Columns.departure_time);
        indexOfStopId = findIndexOf(headers, Columns.stop_id);
        indexOfStopSeq = findIndexOf(headers, Columns.stop_sequence);
        indexOfPickup = findIndexOf(headers, Columns.pickup_type);
        indexOfDropOff = findIndexOf(headers, Columns.drop_off_type);
    }

    public StopTimeData parseEntry(CSVRecord data) {
        String tripId = getTripId(data);
        Optional<ServiceTime> arrivalTime;
        Optional<ServiceTime> departureTime;

        String fieldOne = data.get(indexOfArrival);
        arrivalTime = parseTimeField(fieldOne, tripId);

        String fieldTwo = data.get(indexOfDepart);
        departureTime = parseTimeField(fieldTwo, tripId);

        String stopId = data.get(indexOfStopId);

        String stopSequence = data.get(indexOfStopSeq);
        String pickupType = data.get(indexOfPickup);
        String dropOffType = data.get(indexOfDropOff);

        if (arrivalTime.isEmpty() || departureTime.isEmpty()) {
            logger.error("Failed to parse arrival time from fields " + data);
            throw new RuntimeException("Unable to parse time for " + data);
        }

        return new StopTimeData(tripId, arrivalTime.get(), departureTime.get(), stopId, stopSequence, pickupType, dropOffType);
    }

    private String getTripId(CSVRecord data) {
        return data.get(indexOfId);
    }

    private Optional<ServiceTime> parseTimeField(String fieldOne, String tripId) {
        Optional<ServiceTime> time = Optional.empty();
        if (fieldOne.contains(":")) {
            time = ServiceTime.parseTime(fieldOne);
        }
        if (time.isEmpty()) {
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