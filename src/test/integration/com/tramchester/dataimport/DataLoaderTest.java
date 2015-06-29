package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DataLoaderTest {

    @Test
    public void shouldLoadRouteData() throws Exception {
        DataLoader<RouteData> dataLoader = new DataLoader<>("data/test/routes", new RouteDataParser());
        List<RouteData> routeData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(routeData).hasSize(2);
        assertThat(routeData.get(0).getCode()).isEqualTo("MET1");
        assertThat(routeData.get(0).getId()).isEqualTo("MET:MET1:I:");
        assertThat(routeData.get(0).getName()).isEqualTo("Abraham Moss - Bury");
    }

    @Test
    public void shouldLoadCalendarData() throws Exception {
        DataLoader<CalendarData> dataLoader = new DataLoader<>("data/test/calendar", new CalendarDataParser());
        List<CalendarData> calendarData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(calendarData).hasSize(12);
        assertThat(calendarData.get(0).getServiceId()).isEqualTo("Serv000001");
        assertThat(calendarData.get(0).getStartDate().toString()).contains("2014-10-20");
        assertThat(calendarData.get(0).getEndDate().toString()).contains("2014-12-19");
    }

    @Test
    public void shouldLoadStopData() throws Exception {
        DataLoader<StopData> dataLoader = new DataLoader<>("data/test/stops", new StopDataParser());
        List<StopData> stopData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(stopData).hasSize(8);
        assertThat(stopData.get(0).getCode()).isEqualTo("mantpmaw");
        assertThat(stopData.get(0).getId()).isEqualTo("9400ZZMAABM");
        assertThat(stopData.get(0).getName()).isEqualTo("Abraham Moss");
        assertThat(stopData.get(0).getLatitude()).isEqualTo(53.51046);
        assertThat(stopData.get(0).getLongitude()).isEqualTo(-2.23550);
    }

    @Test
    public void shouldLoadStopTimeData() throws Exception {
        DataLoader<StopTimeData> dataLoader = new DataLoader<>("data/test/stop_times", new StopTimeDataParser());
        List<StopTimeData> stopTimeData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(stopTimeData).hasSize(20);
        assertThat(stopTimeData.get(0).getStopId()).isEqualTo("9400ZZMAABM");
        assertThat(stopTimeData.get(0).getTripId()).isEqualTo("Trip000001");
        assertThat(stopTimeData.get(0).getDropOffType()).isEqualTo("1");
        assertThat(stopTimeData.get(0).getMinutesFromMidnight()).isEqualTo(401);
        assertThat(stopTimeData.get(0).getStopSequence()).isEqualTo("0001");
        assertThat(stopTimeData.get(0).getArrivalTime().toString()).isEqualTo("2000-01-01T06:41:00.000Z");
        assertThat(stopTimeData.get(0).getDepartureTime().toString()).isEqualTo("2000-01-01T06:41:00.000Z");
    }

    @Test
    public void shouldLoadTripData() throws Exception {
        DataLoader<TripData> dataLoader = new DataLoader<>("data/test/trips", new TripDataParser());
        List<TripData> tripData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(tripData).hasSize(20);
        assertThat(tripData.get(0).getTripHeadsign()).isEqualTo("Bury Interchange");
        assertThat(tripData.get(0).getTripId()).isEqualTo("Trip000001");
        assertThat(tripData.get(0).getServiceId()).isEqualTo("Serv000001");
        assertThat(tripData.get(0).getRouteId()).isEqualTo("MET:MET1:I:");
    }

}