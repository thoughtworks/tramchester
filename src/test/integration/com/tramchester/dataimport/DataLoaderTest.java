package com.tramchester.dataimport;

import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DataLoaderTest {

    // the test data files currently manually maintained, copy over from data/tram as needed

    @Test
    public void shouldLoadRouteData() throws Exception {
        DataLoader<RouteData> dataLoader = new DataLoader<>("data/test/routes", new RouteDataParser());
        List<RouteData> routeData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(routeData).hasSize(2);
        RouteData theRoute = routeData.get(0);
        assertThat(theRoute.getCode()).isEqualTo("MET1");
        assertThat(theRoute.getId()).isEqualTo("MET:MET1:I:");
        assertThat(theRoute.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(theRoute.getAgency()).isEqualTo("MET");
    }

    @Test
    public void shouldLoadAgencyData() throws IOException {
        DataLoader<AgencyData> dataLoader = new DataLoader<>("data/test/agency", new AgencyDataParser());
        List<AgencyData> agencyData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(agencyData).hasSize(43);
        agencyData.removeIf(agent -> !agent.getId().equals("MET"));
        assertThat(agencyData).hasSize(1);
        AgencyData anAgent = agencyData.get(0);
        assertThat(anAgent.getName()).isEqualTo("Metrolink");
        assertThat(anAgent.getUrl()).isEqualTo("http://www.tfgm.com");
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

        assertThat(stopData).hasSize(176);
        StopData theStop = stopData.get(0);
        assertThat(theStop.getCode()).isEqualTo("mantpmaw");
        assertThat(theStop.getId()).isEqualTo("9400ZZMAABM1");
        assertThat(theStop.getName()).isEqualTo("Abraham Moss");
        assertThat(theStop.getLatitude()).isEqualTo(53.51046);
        assertThat(theStop.getLongitude()).isEqualTo(-2.23550);
    }

    @Test
    public void shouldLoadStopTimeData() throws Exception {
        DataLoader<StopTimeData> dataLoader = new DataLoader<>("data/test/stop_times", new StopTimeDataParser());
        List<StopTimeData> stopTimeData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(stopTimeData).hasSize(20);
        StopTimeData stopTime = stopTimeData.get(0);
        assertThat(stopTime.getStopId()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTime.getTripId()).isEqualTo("Trip000001");
        assertThat(stopTime.getDropOffType()).isEqualTo("1");
        assertThat(stopTime.getStopSequence()).isEqualTo("0001");
        assertThat(stopTime.getArrivalTime().toString()).isEqualTo("06:41");
        assertThat(stopTime.getDepartureTime().toString()).isEqualTo("06:41");
    }

    @Test
    public void shouldLoadTripData() throws Exception {
        DataLoader<TripData> dataLoader = new DataLoader<>("data/test/trips", new TripDataParser());
        List<TripData> tripData = dataLoader.loadAll().collect(Collectors.toList());

        assertThat(tripData).hasSize(20);
        TripData theTrip = tripData.get(0);
        assertThat(theTrip.getTripHeadsign()).isEqualTo("Bury Interchange");
        assertThat(theTrip.getTripId()).isEqualTo("Trip000001");
        assertThat(theTrip.getServiceId()).isEqualTo("Serv000001");
        assertThat(theTrip.getRouteId()).isEqualTo("MET:MET1:I:");
    }

}