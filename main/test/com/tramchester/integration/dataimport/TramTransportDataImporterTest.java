package com.tramchester.integration.dataimport;

import com.tramchester.config.DownloadConfig;
import com.tramchester.dataimport.TransportDataImporter;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.repository.TransportData;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class TramTransportDataImporterTest {

    private DownloadConfig config = new DownloadConfig() {
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
    public void shouldLoadTransportData() {
        TransportDataReaderFactory factory = new TransportDataReaderFactory(config);
        TransportDataImporter transportDataImporter = new TransportDataImporter(factory);

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
        assertThat(stop.getArrivalTime()).isEqualTo(TramTime.of(06,41));
        assertThat(stop.getGetSequenceNumber()).isEqualTo(1);

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