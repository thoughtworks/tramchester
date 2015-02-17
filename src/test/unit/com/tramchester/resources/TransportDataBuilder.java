package com.tramchester.resources;


import com.tramchester.dataimport.data.*;
import com.tramchester.domain.TransportData;

import java.util.ArrayList;
import java.util.List;

public class TransportDataBuilder {
    private List<StopData> stopDataList = new ArrayList<>();
    private List<RouteData> routeDataList = new ArrayList<>();
    private List<TripData> tripDataList = new ArrayList<>();
    private List<StopTimeData> stopTimeDataList = new ArrayList<>();
    private List<CalendarData> calendarDataList = new ArrayList<>();

    public TransportData build() {
        return new TransportData(stopDataList, routeDataList, tripDataList, stopTimeDataList, calendarDataList);
    }
}
