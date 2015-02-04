package com.tramchester.dataimport;

import com.tramchester.dataimport.parsers.RouteParser;
import com.tramchester.dataimport.parsers.StopParser;
import com.tramchester.dataimport.parsers.StopTimeParser;
import com.tramchester.dataimport.parsers.TripParser;
import com.tramchester.domain.RouteData;
import com.tramchester.domain.StopData;
import com.tramchester.domain.TransportData;
import com.tramchester.domain.TripData;

import java.util.List;

public class TransportDataImporter {

    public TransportData load() {
        List<StopData> stopDatas = getStops();
        List<RouteData> routeDatas = getRoutes();
        List<TripData> tripDatas = getTrips();
        List<StopTime> stopTimes = getStopTimes();

        return new TransportData(stopDatas, routeDatas, tripDatas, stopTimes);
    }

    private List<StopTime> getStopTimes() {
        DataLoader<StopTime> tripLoader = new DataLoader<>("stop_times", new StopTimeParser());
        return tripLoader.loadAll();    }

    private List<TripData> getTrips() {
        DataLoader<TripData> tripLoader = new DataLoader<>("trips", new TripParser());
        return tripLoader.loadAll();
    }

    private List<StopData> getStops() {
        DataLoader<StopData> stopLoader = new DataLoader<>("stops", new StopParser());
        return stopLoader.loadAll();
    }

    public List<RouteData> getRoutes() {
        DataLoader<RouteData> routeLoader = new DataLoader<>("routes", new RouteParser());
        return routeLoader.loadAll();
    }
}

