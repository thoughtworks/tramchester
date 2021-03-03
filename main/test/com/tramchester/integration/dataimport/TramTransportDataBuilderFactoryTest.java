package com.tramchester.integration.dataimport;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.dataimport.FetchFileModTime;
import com.tramchester.dataimport.TransportDataFromFilesBuilder;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.reference.GTFSTransportationType;
import com.tramchester.domain.reference.TransportMode;
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

        CsvMapper mapper = CsvMapper.builder().build();

        TransportDataReaderFactory factory = new TransportDataReaderFactory(testConfig, fetchFileModTime, mapper);

        ProvidesNow providesNow = new ProvidesLocalNow();

        TransportDataFromFilesBuilder transportDataImporter = new TransportDataFromFilesBuilder(factory, providesNow,
                testConfig);

        TransportDataFromFiles builder = transportDataImporter.create();
        //builder.register(added::add);
        //builder.load();
        TransportData transportData = builder.getData();

        assertThat(transportData.getRoutes()).hasSize(2);

        Route route = transportData.getRouteById(StringIdFor.createId("MET:MET1:I:"));
        assertThat(route.getName()).isEqualTo("Abraham Moss - Bury");
        assertThat(route.getServices()).hasSize(2);

        Service service = route.getServices().stream().findFirst().get();
        assertThat(service.getId()).isEqualTo(StringIdFor.createId("Serv000001"));
        assertThat(service.getTrips()).hasSize(3);
        assertThat(service.getCalendar().operatesOn(LocalDate.of(2014,11,1)));
        assertThat(!service.getCalendar().operatesOn(LocalDate.of(2014,11,20)));

        Trip trip = service.getTrips().stream().findFirst().get();
        assertThat(trip.getId()).isEqualTo(StringIdFor.createId("Trip000001"));
        assertThat(trip.getStops().numberOfCallingPoints()).isEqualTo(9);

        StopCall stop = trip.getStops().getStopBySequenceNumber(1);
        assertThat(stop.getStation().getName()).isEqualTo("Abraham Moss");
        assertThat(stop.getArrivalTime()).isEqualTo(TramTime.of(6, 41));
        assertThat(stop.getGetSequenceNumber()).isEqualTo(1);

        Set<DataSourceInfo> dataSourceInfo = transportData.getDataSourceInfo();
        assertEquals(1, dataSourceInfo.size());
        DataSourceInfo result = dataSourceInfo.iterator().next();
        assertThat(result.getVersion()).isEqualTo("20150617");
        assertThat(result.getID()).isEqualTo(DataSourceID.TFGM());

        FeedInfo feedInfo = transportData.getFeedInfos().get(DataSourceID.TFGM());
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
            super(dataFolder, Collections.singleton(GTFSTransportationType.tram), Collections.singleton(TransportMode.Tram));
        }

        @Override
        public Path getUnzipPath() {
            return Paths.get("test");
        }
    }

}