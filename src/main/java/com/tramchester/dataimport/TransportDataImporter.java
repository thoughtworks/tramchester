package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.TransportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class TransportDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataImporter.class);
    private final TransportDataReader transportDataReader;

    public TransportDataImporter(TransportDataReader transportDataReader) {
        this.transportDataReader = transportDataReader;
    }

    public TransportData load() {
        try {
            List<StopData> stopData = transportDataReader.getStops();
            List<RouteData> routeData = transportDataReader.getRoutes();
            List<TripData> tripData = transportDataReader.getTrips();
            List<StopTimeData> stopTimeData = transportDataReader.getStopTimes();
            List<CalendarData> calendarData = transportDataReader.getCalendar();
            logger.info("Finished reading csv files.");
            return new TransportData(stopData, routeData, tripData, stopTimeData, calendarData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

