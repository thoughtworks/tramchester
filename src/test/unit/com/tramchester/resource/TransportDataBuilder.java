package com.tramchester.resource;


import com.tramchester.dataimport.data.*;
import com.tramchester.domain.TransportData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TransportDataBuilder {
    private Stream<StopData> stopDataList = Stream.empty();
    private Stream<RouteData> routeDataList = Stream.empty();
    private Stream<TripData> tripDataList = Stream.empty();
    private Stream<StopTimeData> stopTimeDataList = Stream.empty();
    private Stream<CalendarData> calendarDataList = Stream.empty();

    public TransportData build() {
        return new TransportData(stopDataList, routeDataList, tripDataList, stopTimeDataList, calendarDataList);
    }
}
