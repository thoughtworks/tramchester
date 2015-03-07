package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class TransportDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataImporter.class);
    private final TransportDataReader transportDataReader;

    public TransportDataImporter(TransportDataReader transportDataReader) {
        this.transportDataReader = transportDataReader;
    }

    public TransportData load() {
        try {
            Stream<StopData> stopData = transportDataReader.getStops();
            Stream<RouteData> routeData = transportDataReader.getRoutes();
            Stream<TripData> tripData = transportDataReader.getTrips();
            Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes();
            Stream<CalendarData> calendarData = transportDataReader.getCalendar();
            logger.info("Finished reading csv files.");
            return new TransportData(stopData, routeData, tripData, stopTimeData, calendarData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

