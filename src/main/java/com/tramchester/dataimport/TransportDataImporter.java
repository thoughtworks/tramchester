package com.tramchester.dataimport;

import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.dataimport.parsers.RouteDataParser;
import com.tramchester.dataimport.parsers.StopDataParser;
import com.tramchester.dataimport.parsers.StopTimeDataParser;
import com.tramchester.dataimport.parsers.TripDataParser;
import com.tramchester.dataimport.data.RouteData;
import com.tramchester.dataimport.data.StopData;
import com.tramchester.domain.TransportData;
import com.tramchester.dataimport.data.TripData;

import java.io.IOException;
import java.util.List;

public class TransportDataImporter {

    public TransportData load() {
        try {
            List<StopData> stopData = getStops();
            List<RouteData> routeData = getRoutes();
            List<TripData> tripData = getTrips();
            List<StopTimeData> stopTimeData = getStopTimes();

            return new TransportData(stopData, routeData, tripData, stopTimeData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

