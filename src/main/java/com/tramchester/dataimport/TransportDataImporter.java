package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.TransportData;

import java.io.IOException;
import java.util.List;

public class TransportDataImporter {

    public TransportData load() {
        try {
            List<StopData> stopData = getStops();
            List<RouteData> routeData = getRoutes();
            List<TripData> tripData = getTrips();
            List<StopTimeData> stopTimeData = getStopTimes();
            List<CalendarData> calendarData = getCalendar();

            return new TransportData(stopData, routeData, tripData, stopTimeData, calendarData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<CalendarData> getCalendar() throws IOException {
        return new DataLoader<>("calendar", new CalendarDataParser()).loadAll();
    }

    private List<StopTimeData> getStopTimes() throws IOException {
        return new DataLoader<>("stop_times", new StopTimeDataParser()).loadAll();
    }

    private List<TripData> getTrips() throws IOException {
        return new DataLoader<>("trips", new TripDataParser()).loadAll();
    }

    private List<StopData> getStops() throws IOException {
        return new DataLoader<>("stops", new StopDataParser()).loadAll();
    }

    public List<RouteData> getRoutes() throws IOException {
        return new DataLoader<>("routes", new RouteDataParser()).loadAll();
    }
}

