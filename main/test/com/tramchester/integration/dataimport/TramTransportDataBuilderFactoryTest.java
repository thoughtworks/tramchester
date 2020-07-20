package com.tramchester.integration.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.TransportDataBuilderFactory;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.domain.DataSourceInfo;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataFromFilesBuilder;
import com.tramchester.repository.TransportDataSource;
import com.tramchester.testSupport.TestConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramTransportDataBuilderFactoryTest {

    private static class DownloadConfig implements DataSourceConfig {

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
            return Paths.get("data");
        }

        @Override
        public Path getUnzipPath() {
            return Paths.get("test");
        }

        @Override
        public String getZipFilename() {
            return "zipfilename.zip";
        }

        @Override
        public String getName() {
            return "TramTransportDataBuilderFactoryTest.DownloadConfig";
        }

        @Override
        public boolean getHasFeedInfo() {
            return true;
        }
    }

    @Test
    void shouldLoadTransportData() {

        TestConfig testConfig = new TestConfig() {
            @Override
            protected DataSourceConfig getTestDataSourceConfig() {
                return new DownloadConfig();
            }
        };

//        DownloadConfig downloadConfig = new DownloadConfig();

        FetchFileModTime fetchFileModTime = new FetchFileModTime();
        TransportDataReaderFactory factory = new TransportDataReaderFactory(testConfig, fetchFileModTime);

        ProvidesNow providesNow = new ProvidesLocalNow();
        CoordinateTransforms coordinateTransforms = new CoordinateTransforms();
        StationLocations stationLocations = new StationLocations(coordinateTransforms);

        TransportDataBuilderFactory transportDataImporter = new TransportDataBuilderFactory(factory, providesNow,
                stationLocations, testConfig);

        TransportDataFromFilesBuilder builder = transportDataImporter.create();
        builder.load();
        TransportDataSource transportData = builder.getData();

        assertThat(transportData.getRoutes()).hasSize(2);

        Route route = transportData.getRoute("MET:MET1:I:");
        assertThat(route.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(route.getServices()).hasSize(2);

        Service service = route.getServices().stream().findFirst().get();
        assertThat(service.getId()).isEqualTo("Serv000001");
        assertThat(service.getAllTrips()).hasSize(3);
        assertThat(service.operatesOn(LocalDate.of(2014,11,1)));
        assertThat(!service.operatesOn(LocalDate.of(2014,11,20)));

        Trip trip = service.getAllTrips().stream().findFirst().get();
        assertThat(trip.getId()).isEqualTo("Trip000001");
        assertThat(trip.getStops()).hasSize(9);

        StopCall stop = trip.getStops().get(0);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime()).isEqualTo(ServiceTime.of(6,41));
        assertThat(stop.getGetSequenceNumber()).isEqualTo(1);

        DataSourceInfo feedInfo = transportData.getDataSourceInfo();
        Set<DataSourceInfo.NameAndVersion> versions = feedInfo.getVersions();
        assertEquals(1, versions.size());
        DataSourceInfo.NameAndVersion result = versions.iterator().next();
        assertThat(result.getVersion()).isEqualTo("20150617");
        assertThat(result.getName()).isEqualTo("TramTransportDataBuilderFactoryTest.DownloadConfig");

//        assertThat(feedInfo.getPublisherName()).isEqualTo("Transport for Greater Manchester");
//        assertThat(feedInfo.getPublisherUrl()).isEqualTo("http://www.tfgm.com");
//        assertThat(feedInfo.getTimezone()).isEqualTo("Europe/London");
//        assertThat(feedInfo.getLang()).isEqualTo("en");
//        assertThat(feedInfo.getVersion()).isEqualTo("20150617");
//        assertThat(feedInfo.validFrom()).isEqualTo(LocalDate.of(2015,6,18));
//        assertThat(feedInfo.validUntil()).isEqualTo(LocalDate.of(2015,8,18));
    }
}