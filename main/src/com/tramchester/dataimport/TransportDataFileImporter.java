package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class TransportDataFileImporter {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFileImporter.class);

    private final TransportDataReader transportDataReader;
    private final ProvidesNow providesNow;
    private final StationLocations stationLocations;

    public TransportDataFileImporter(TransportDataReaderFactory factory, ProvidesNow providesNow,
                                     StationLocations stationLocations) {
        this.transportDataReader = factory.getForLoader();
        this.providesNow = providesNow;
        this.stationLocations = stationLocations;
    }

    public TransportDataFromFiles createSource() {
        Set<String> includeAll = Collections.emptySet();

        Stream<StopData> stopData = transportDataReader.getStops(new StopDataMapper(includeAll));
        Stream<RouteData> routeData = transportDataReader.getRoutes(new RouteDataMapper(includeAll, false));
        Stream<TripData> tripData = transportDataReader.getTrips(new TripDataMapper(includeAll));
        Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes(new StopTimeDataMapper(includeAll));
        Stream<CalendarData> calendarData = transportDataReader.getCalendar(new CalendarDataMapper(includeAll));
        Stream<FeedInfo> feedInfoData = transportDataReader.getFeedInfo(new FeedInfoDataMapper(providesNow));

        logger.info("Finished reading csv files.");
        return new TransportDataFromFiles(stationLocations, stopData, routeData, tripData, stopTimeData, calendarData,
                feedInfoData);

    }
}

