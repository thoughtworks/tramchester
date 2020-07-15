package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFiles;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

public class TransportDataFromFileFactory {

    private final TransportDataReader transportDataReader;
    private final ProvidesNow providesNow;
    private final StationLocations stationLocations;

    public TransportDataFromFileFactory(TransportDataLoader providesLoader, ProvidesNow providesNow,
                                        StationLocations stationLocations) {
        this.transportDataReader = providesLoader.getForLoader();
        this.providesNow = providesNow;
        this.stationLocations = stationLocations;
    }

    // feedinfo is not mandatory in the standard
    public TransportDataFromFiles create(boolean expectFeedinfo) {
        Set<String> includeAll = Collections.emptySet();

        // streams, so no data read yet
        Stream<FeedInfo> feedInfoData = transportDataReader.getFeedInfo(expectFeedinfo, new FeedInfoDataMapper(providesNow));

        Stream<StopData> stopData = transportDataReader.getStops(new StopDataMapper(includeAll));
        Stream<RouteData> routeData = transportDataReader.getRoutes(new RouteDataMapper(includeAll, false));
        Stream<TripData> tripData = transportDataReader.getTrips(new TripDataMapper(includeAll));
        Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes(new StopTimeDataMapper(includeAll));
        Stream<CalendarData> calendarData = transportDataReader.getCalendar(new CalendarDataMapper(includeAll));
        Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates(new CalendarDatesDataMapper(includeAll));
        Stream<AgencyData> agencyData = transportDataReader.getAgencies(new AgencyDataMapper(includeAll));

        TransportDataFromFiles.TransportDataStreams transportDataStreams =
                new TransportDataFromFiles.TransportDataStreams(agencyData, stopData, routeData, tripData,
                    stopTimeData, calendarData, feedInfoData, calendarsDates);

        return new TransportDataFromFiles(stationLocations, transportDataStreams);

    }
}

