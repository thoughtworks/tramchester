package com.tramchester.dataimport.parsers;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.GTFSPickupDropoffType;
import com.tramchester.domain.time.ServiceTime;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.tramchester.domain.GTFSPickupDropoffType.Regular;
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
        try {
            String tripId = getTripId(data);

            ServiceTime arrivalTime = parseTimeField(data.get(indexOfArrival));
            ServiceTime departureTime = parseTimeField(data.get(indexOfDepart));

            String stopId = data.get(indexOfStopId);
            int stopSequence = Integer.parseInt(data.get(indexOfStopSeq));

            GTFSPickupDropoffType pickupType = GTFSPickupDropoffType.parser.parse(data.get(indexOfPickup));
            GTFSPickupDropoffType dropOffType = GTFSPickupDropoffType.parser.parse(data.get(indexOfDropOff));

            // seems normal for the train data, has all stations even ones don't stop at
//            if (pickupType!=Regular && dropOffType!=Regular) {
//                logger.info("No drop-off or pickup for trip " + tripId + " and stop " + stopId);
//            }

            return new StopTimeData(tripId, arrivalTime, departureTime, stopId, stopSequence, pickupType, dropOffType);
        }
        catch(ParseException|NumberFormatException parseException) {
            logger.error("Failed to parse arrival time from fields " + data);
            throw new RuntimeException("Unable to parse time for " + data, parseException);
        }
    }

    private String getTripId(CSVRecord data) {
        return data.get(indexOfId);
    }

    private ServiceTime parseTimeField(String theText) throws ParseException {
        Optional<ServiceTime> time = Optional.empty();
        if (theText.contains(":")) {
            time = ServiceTime.parseTime(theText);
        }
        if (time.isEmpty()) {
            String msg = format("Failed to parse time '%s' ", theText);
            logger.error(msg);
            throw new ParseException(msg, 0);
        }
        return time.get();
    }

    @Override
    public boolean shouldInclude(CSVRecord data) {
        if (includeAll) {
            return true;
        }
        return tripIds.contains(getTripId(data));
    }


}