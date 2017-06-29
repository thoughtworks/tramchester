package com.tramchester.integration.dataimport;

import com.tramchester.integration.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.integration.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.stream.Stream;

public class TransportDataImporter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataImporter.class);
    private final TransportDataReader transportDataReader;

    public TransportDataImporter(TransportDataReader transportDataReader) {
        this.transportDataReader = transportDataReader;
    }

    public TransportDataFromFiles load() {
        try {
            Stream<StopData> stopData = transportDataReader.getStops();
            Stream<RouteData> routeData = transportDataReader.getRoutes();
            Stream<TripData> tripData = transportDataReader.getTrips();
            Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes();
            Stream<CalendarData> calendarData = transportDataReader.getCalendar();
            Stream<FeedInfo> feedInfoData = transportDataReader.getFeedInfo();

            logger.info("Finished reading csv files.");
            return new TransportDataFromFiles(stopData, routeData, tripData, stopTimeData, calendarData, feedInfoData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

