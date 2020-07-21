package com.tramchester.integration.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.TransportDataBuilderFactory;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.ServiceTime;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.TFGMTestDataSourceConfig;
import com.tramchester.repository.TransportDataFromFilesBuilderGeoFilter;
import com.tramchester.repository.TransportDataSource;
import com.tramchester.testSupport.TestConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramTransportDataBuilderFactoryTest {

    @Test
    void shouldLoadTransportData() {

        TestConfig testConfig = new TestConfig() {
            @Override
            protected DataSourceConfig getTestDataSourceConfig() {
                return new SourceConfig("data");
            }
        };

        FetchFileModTime fetchFileModTime = new FetchFileModTime();
        TransportDataReaderFactory factory = new TransportDataReaderFactory(testConfig, fetchFileModTime);

        ProvidesNow providesNow = new ProvidesLocalNow();
        StationLocations stationLocations = new StationLocations();

        TransportDataBuilderFactory transportDataImporter = new TransportDataBuilderFactory(factory, providesNow,
                stationLocations, testConfig);

        TransportDataFromFilesBuilderGeoFilter builder = transportDataImporter.create();
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
        assertThat(trip.getStops().numberOfCallingPoints()).isEqualTo(9);

        StopCall stop = trip.getStops().getStopBySequenceNumber(1);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime()).isEqualTo(ServiceTime.of(6,41));
        assertThat(stop.getGetSequenceNumber()).isEqualTo(1);

        DataSourceInfo dataSourceInfo = transportData.getDataSourceInfo();
        Set<DataSourceInfo.NameAndVersion> versions = dataSourceInfo.getVersions();
        assertEquals(1, versions.size());
        DataSourceInfo.NameAndVersion result = versions.iterator().next();
        assertThat(result.getVersion()).isEqualTo("20150617");
        assertThat(result.getName()).isEqualTo("tfgm");

        FeedInfo feedInfo = transportData.getFeedInfos().get("tfgm");
        assertThat(feedInfo.getPublisherName()).isEqualTo("Transport for Greater Manchester");
        assertThat(feedInfo.getPublisherUrl()).isEqualTo("http://www.tfgm.com");
        assertThat(feedInfo.getTimezone()).isEqualTo("Europe/London");
        assertThat(feedInfo.getLang()).isEqualTo("en");
        assertThat(feedInfo.getVersion()).isEqualTo("20150617");
        assertThat(feedInfo.validFrom()).isEqualTo(LocalDate.of(2015,6,18));
        assertThat(feedInfo.validUntil()).isEqualTo(LocalDate.of(2015,8,18));
    }

    private static class SourceConfig extends TFGMTestDataSourceConfig {
        public SourceConfig(String dataFolder) {
            super(dataFolder, Collections.singleton(GTFSTransportationType.tram));
        }

        @Override
        public Path getUnzipPath() {
            return Paths.get("test");
        }
    }

}