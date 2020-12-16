package com.tramchester.integration.dataimport;

import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.TransportDataFromFilesBuilder;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.TFGMTestDataSourceConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.TestConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TramTransportDataBuilderFactoryTest {

    @Test
    void shouldLoadTransportData() {

        TestConfig testConfig = new TestConfig() {
            @Override
            protected List<DataSourceConfig> getDataSourceFORTESTING() {
                return Collections.singletonList(new SourceConfig("data"));
            }
        };

        FetchFileModTime fetchFileModTime = new FetchFileModTime();
        TransportDataReaderFactory factory = new TransportDataReaderFactory(testConfig, fetchFileModTime);

        ProvidesNow providesNow = new ProvidesLocalNow();

        TransportDataFromFilesBuilder transportDataImporter = new TransportDataFromFilesBuilder(factory, providesNow,
                testConfig);

        TransportDataFromFiles builder = transportDataImporter.create();
        //builder.register(added::add);
        //builder.load();
        TransportData transportData = builder.getData();

        assertThat(transportData.getRoutes()).hasSize(2);

        Route route = transportData.getRouteById(IdFor.createId("MET:MET1:I:"));
        assertThat(route.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(route.getServices()).hasSize(2);

        Service service = route.getServices().stream().findFirst().get();
        assertThat(service.getId()).isEqualTo(IdFor.createId("Serv000001"));
        assertThat(service.getAllTrips()).hasSize(3);
        assertThat(service.operatesOn(LocalDate.of(2014,11,1)));
        assertThat(!service.operatesOn(LocalDate.of(2014,11,20)));

        Trip trip = service.getAllTrips().stream().findFirst().get();
        assertThat(trip.getId()).isEqualTo(IdFor.createId("Trip000001"));
        assertThat(trip.getStops().numberOfCallingPoints()).isEqualTo(9);

        StopCall stop = trip.getStops().getStopBySequenceNumber(1);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime()).isEqualTo(TramTime.of(6, 41));
        assertThat(stop.getGetSequenceNumber()).isEqualTo(1);

        Set<DataSourceInfo> dataSourceInfo = transportData.getDataSourceInfo();
        //Set<NameAndVersion> versions = dataSourceInfo.getVersions();
        assertEquals(1, dataSourceInfo.size());
        DataSourceInfo result = dataSourceInfo.iterator().next();
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