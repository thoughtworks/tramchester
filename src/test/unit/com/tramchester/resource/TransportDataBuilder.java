package com.tramchester.resource;


import com.tramchester.dataimport.data.*;
import com.tramchester.domain.TransportDataFromFiles;

import java.util.stream.Stream;

public class TransportDataBuilder {
    private Stream<StopData> stopDataList = Stream.empty();
    private Stream<RouteData> routeDataList = Stream.empty();
    private Stream<TripData> tripDataList = Stream.empty();
    private Stream<StopTimeData> stopTimeDataList = Stream.empty();
    private Stream<CalendarData> calendarDataList = Stream.empty();

    public TransportDataFromFiles build() {
        return new TransportDataFromFiles(stopDataList, routeDataList, tripDataList, stopTimeDataList, calendarDataList);
    }
}
