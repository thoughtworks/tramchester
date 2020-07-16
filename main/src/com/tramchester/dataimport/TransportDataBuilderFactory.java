package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFilesBuilder;

import java.util.*;
import java.util.stream.Stream;

public class TransportDataBuilderFactory {

    private final List<TransportDataReader> transportDataReaders;
    private final ProvidesNow providesNow;
    private final StationLocations stationLocations;

    public TransportDataBuilderFactory(TransportDataLoader providesLoader, ProvidesNow providesNow,
                                       StationLocations stationLocations) {
        this.transportDataReaders = providesLoader.getReaders();
        this.providesNow = providesNow;
        this.stationLocations = stationLocations;
    }

    // feedinfo is not mandatory in the standard
    public TransportDataFromFilesBuilder create() {
        // streams, so no data read yet

        Set<String> includeAll = Collections.emptySet();
        List<TransportDataFromFilesBuilder.TransportDataStreams> dataStreams = new ArrayList<>();

        transportDataReaders.forEach(transportDataReader -> {
            Stream<FeedInfo> feedInfoData = Stream.empty();
            boolean expectFeedinfo = transportDataReader.getExpectFeedinfo();
            if (expectFeedinfo) {
                feedInfoData = transportDataReader.getFeedInfo(new FeedInfoDataMapper(providesNow));
            }

            Stream<StopData> stopData = transportDataReader.getStops(new StopDataMapper(includeAll));
            Stream<RouteData> routeData = transportDataReader.getRoutes(new RouteDataMapper(includeAll, false));
            Stream<TripData> tripData = transportDataReader.getTrips(new TripDataMapper(includeAll));
            Stream<StopTimeData> stopTimeData = transportDataReader.getStopTimes(new StopTimeDataMapper(includeAll));
            Stream<CalendarData> calendarData = transportDataReader.getCalendar(new CalendarDataMapper(includeAll));
            Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates(new CalendarDatesDataMapper(includeAll));
            Stream<AgencyData> agencyData = transportDataReader.getAgencies(new AgencyDataMapper(includeAll));

            TransportDataFromFilesBuilder.TransportDataStreams transportDataStreams =
                    new TransportDataFromFilesBuilder.TransportDataStreams(agencyData, stopData, routeData, tripData,
                            stopTimeData, calendarData, feedInfoData, calendarsDates, expectFeedinfo);
            dataStreams.add(transportDataStreams);
        });

        return new TransportDataFromFilesBuilder(dataStreams, stationLocations);
    }
}

