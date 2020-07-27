package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.DataLoader;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.GTFSPickupDropoffType;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.time.ServiceTime;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TramDataLoaderTest {

    // the test data files currently manually maintained, copy over from data/tram as needed

    @Test
    void shouldLoadRouteData() {
        HashSet<String> agencies = new HashSet<>(Collections.singletonList("MET"));
        DataLoader<RouteData> dataLoader = new DataLoader<>(Path.of("data/test/routes.txt"), new RouteDataMapper(agencies, true));
        List<RouteData> routeData = dataLoader.loadFiltered(true).collect(Collectors.toList());

        assertThat(routeData).hasSize(2);
        RouteData theRoute = routeData.get(0);
        assertThat(theRoute.getShortName()).isEqualTo("MET1");
        assertThat(theRoute.getId()).isEqualTo(IdFor.createId("MET:MET1:I:"));
        assertThat(theRoute.getLongName()).isEqualTo("Abraham Moss - Bury");
        assertThat(theRoute.getAgencyId()).isEqualTo(IdFor.createId("MET"));
    }

    @Test
    void shouldLoadCalendarData() {
        DataLoader<CalendarData> dataLoader = new DataLoader<>(Path.of("data/test/calendar.txt"), new CalendarDataMapper(Collections.emptySet()));
        List<CalendarData> calendarData = dataLoader.loadFiltered(true).collect(Collectors.toList());

        assertThat(calendarData).hasSize(3);
        assertThat(calendarData.get(0).getServiceId()).isEqualTo(IdFor.createId("Serv000001"));
        assertThat(calendarData.get(0).getStartDate().toString()).contains("2014-10-20");
        assertThat(calendarData.get(0).getEndDate().toString()).contains("2014-12-19");
    }

    @Test
    void shouldLoadStopData() {
        StopDataMapper stopDataMapper = new StopDataMapper(Collections.emptySet());
        DataLoader<StopData> dataLoader = new DataLoader<>(Path.of("data/test/stops.txt"), stopDataMapper);

        List<StopData> stopData = dataLoader.loadFiltered(true).collect(Collectors.toList());

        assertThat(stopData).hasSize(178);
        StopData theStop = stopData.get(0);
        assertThat(theStop.getCode()).isEqualTo("mantpmaw");
        assertThat(theStop.getId()).isEqualTo("9400ZZMAABM1");
        assertThat(theStop.getName()).isEqualTo("Abraham Moss");
        assertThat(theStop.getLatLong().getLat()).isEqualTo(53.51046);
        assertThat(theStop.getLatLong().getLon()).isEqualTo(-2.23550);
    }

    @Test
    void shouldLoadStopTimeData() {
        DataLoader<StopTimeData> dataLoader = new DataLoader<>(Path.of("data/test/stop_times.txt"),
                new StopTimeDataMapper(Collections.emptySet()));
        List<StopTimeData> stopTimeData = dataLoader.loadFiltered(true).collect(Collectors.toList());

        assertThat(stopTimeData).hasSize(40);
        StopTimeData stopTime = stopTimeData.get(0);
        assertThat(stopTime.getStopId()).isEqualTo("9400ZZMAABM1");
        assertThat(stopTime.getTripId()).isEqualTo(IdFor.createId("Trip000001"));
        assertThat(stopTime.getDropOffType()).isEqualTo(GTFSPickupDropoffType.None);
        assertThat(stopTime.getPickupType()).isEqualTo(GTFSPickupDropoffType.Regular);
        assertThat(stopTime.getStopSequence()).isEqualTo(1);
        assertThat(stopTime.getArrivalTime()).isEqualTo(ServiceTime.of(6,41));
        assertThat(stopTime.getDepartureTime()).isEqualTo(ServiceTime.of(6,41));
    }

    @Test
    void shouldLoadTripData() {
        DataLoader<TripData> dataLoader = new DataLoader<>(Path.of("data/test/trips.txt"), new TripDataMapper(Collections.emptySet()));
        List<TripData> tripData = dataLoader.loadFiltered(true).collect(Collectors.toList());

        assertThat(tripData).hasSize(6);
        TripData theTrip = tripData.get(0);
        assertThat(theTrip.getTripHeadsign()).isEqualTo("Bury Interchange");
        assertThat(theTrip.getTripId()).isEqualTo(IdFor.createId("Trip000001"));
        assertThat(theTrip.getServiceId()).isEqualTo(IdFor.createId("Serv000001"));
        assertThat(theTrip.getRouteId()).isEqualTo(IdFor.createId("MET:MET1:I:"));
    }

}