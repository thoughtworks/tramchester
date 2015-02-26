package com.tramchester.dataimport;

import com.tramchester.domain.*;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TransportDataImporterTest {
    private static String PATH = "data/test/";

    @Test
    public void shouldLoadTransportData() {
        TransportDataReader transportDataReader = new TransportDataReader(PATH);
        TransportDataImporter transportDataImporter = new TransportDataImporter(transportDataReader);

        TransportData transportData = transportDataImporter.load();

        assertThat(transportData.getRoutes()).hasSize(2);

        Route route = transportData.getRoutes().get("MET:MET1:I:");
        assertThat(route.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(route.getServices()).hasSize(20);

        Service service = route.getServices().get(0);
        assertThat(service.getServiceId()).isEqualTo("Serv000001");
        assertThat(service.getTrips()).hasSize(20);

        Trip trip = service.getTrips().get(0);
        assertThat(trip.getTripId()).isEqualTo("Trip000001");
        assertThat(trip.getStops()).hasSize(9);

        Stop stop = trip.getStops().get(0);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime().toString()).isEqualTo("2000-01-01T06:41:00.000Z");
    }
}