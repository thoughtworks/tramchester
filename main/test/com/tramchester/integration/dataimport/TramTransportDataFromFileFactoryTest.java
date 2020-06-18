package com.tramchester.integration.dataimport;

import com.tramchester.config.DownloadConfig;
import com.tramchester.dataimport.TransportDataFromFileFactory;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.input.Trip;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFiles;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TramTransportDataFromFileFactoryTest {

    private final DownloadConfig config = new DownloadConfig() {

        @Override
        public String getTramDataUrl() {
            return null;
        }

        @Override
        public String getTramDataCheckUrl() {
            return null;
        }

        @Override
        public Path getDataPath() {
            return Paths.get("data","test");
        }

        @Override
        public Path getUnzipPath() {
            return Paths.get("test");
        }
    };

    @Test
    void shouldLoadTransportData() {
        TransportDataReaderFactory factory = new TransportDataReaderFactory(config);
        ProvidesNow providesNow = new ProvidesLocalNow();
        CoordinateTransforms coordinateTransforms = new CoordinateTransforms();
        StationLocations stationLocations = new StationLocations(coordinateTransforms);

        TransportDataFromFileFactory transportDataImporter = new TransportDataFromFileFactory(factory, providesNow,
                stationLocations);

        TransportDataFromFiles transportData = transportDataImporter.create();
        transportData.start();

        assertThat(transportData.getRoutes()).hasSize(2);

        Route route = transportData.getRoute("MET:MET1:I:");
        assertThat(route.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(route.getServices()).hasSize(2);

        Service service = route.getServices().stream().findFirst().get();
        assertThat(service.getId()).isEqualTo("Serv000001");
        assertThat(service.getTrips()).hasSize(3);
        assertThat(service.operatesOn(LocalDate.of(2014,11,1)));
        assertThat(!service.operatesOn(LocalDate.of(2014,11,20)));

        Trip trip = service.getTrips().stream().findFirst().get();
        assertThat(trip.getId()).isEqualTo("Trip000001");
        assertThat(trip.getStops()).hasSize(9);

        StopCall stop = trip.getStops().get(0);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime()).isEqualTo(TramTime.of(06,41));
        assertThat(Byte.toUnsignedInt(stop.getGetSequenceNumber())).isEqualTo(1);

        FeedInfo feedInfo = transportData.getFeedInfo();
        assertThat(feedInfo.getPublisherName()).isEqualTo("Transport for Greater Manchester");
        assertThat(feedInfo.getPublisherUrl()).isEqualTo("http://www.tfgm.com");
        assertThat(feedInfo.getTimezone()).isEqualTo("Europe/London");
        assertThat(feedInfo.getLang()).isEqualTo("en");
        assertThat(feedInfo.getVersion()).isEqualTo("20150617");
        assertThat(feedInfo.validFrom()).isEqualTo(LocalDate.of(2015,6,18));
        assertThat(feedInfo.validUntil()).isEqualTo(LocalDate.of(2015,8,18));
    }
}