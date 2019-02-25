package com.tramchester.unit.resource;


import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.repository.TransportDataFromFiles;

import java.util.stream.Stream;

public class TransportDataBuilder {
    private Stream<StopData> stopDataList = Stream.empty();
    private Stream<RouteData> routeDataList = Stream.empty();
    private Stream<TripData> tripDataList = Stream.empty();
    private Stream<StopTimeData> stopTimeDataList = Stream.empty();
    private Stream<CalendarData> calendarDataList = Stream.empty();
    private Stream<FeedInfo> feedInfoDataList = Stream.empty();

    public TransportDataFromFiles build() {
        return new TransportDataFromFiles(stopDataList, routeDataList, tripDataList, stopTimeDataList,
                calendarDataList, feedInfoDataList);
    }
}
