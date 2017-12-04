package com.tramchester.integration.dataimport;

import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.domain.*;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.repository.TransportData;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class TramTransportDataImporterTest {
    private static String PATH = "data/test/";

    @Test
    public void shouldLoadTransportData() {
        TransportDataReader transportDataReader = new TransportDataReader(Paths.get(PATH));
        TransportDataImporter transportDataImporter = new TransportDataImporter(transportDataReader);

        TransportData transportData = transportDataImporter.load();

        assertThat(transportData.getRoutes()).hasSize(2);

        Route route = transportData.getRoute("MET:MET1:I:");
        assertThat(route.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(route.getServices()).hasSize(1); // 20 trips all for same svc

        Service service = route.getServices().stream().findFirst().get();
        assertThat(service.getServiceId()).isEqualTo("Serv000001");
        assertThat(service.getTrips()).hasSize(20);

        Trip trip = service.getTrips().stream().findFirst().get();
        assertThat(trip.getTripId()).isEqualTo("Trip000001");
        assertThat(trip.getStops()).hasSize(9);

        Stop stop = trip.getStops().get(0);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime()).isEqualTo(new LocalTime(06,41));

        FeedInfo feedInfo = transportData.getFeedInfo();
        assertThat(feedInfo.getPublisherName()).isEqualTo("Transport for Greater Manchester");
        assertThat(feedInfo.getPublisherUrl()).isEqualTo("http://www.tfgm.com");
        assertThat(feedInfo.getTimezone()).isEqualTo("Europe/London");
        assertThat(feedInfo.getLang()).isEqualTo("en");
        assertThat(feedInfo.getVersion()).isEqualTo("20150617");
        assertThat(feedInfo.validFrom()).isEqualTo(new LocalDate(2015,6,18));
        assertThat(feedInfo.validUntil()).isEqualTo(new LocalDate(2015,8,18));
    }
}