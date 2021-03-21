package com.tramchester.integration.repository.trains;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.DataSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.TransportDataLoaderFiles;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.integration.testSupport.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.ArrivaTrainsWales;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static org.junit.jupiter.api.Assertions.*;

@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class TransportDataFromFilesTrainTest {

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder<>().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
        allServices = transportData.getServices();
    }

    @Test
    void shouldHaveExpectedNumbersForTrain() {
        assertEquals(28, transportData.getAgencies().size());
        assertEquals(2578,transportData.getStations().size());
        assertEquals(3635, transportData.getRoutes().size());

        // no platforms represented in train data
        assertEquals(0, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetAgencies() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(28, agencies.size());
        assertTrue(agencies.contains(ArrivaTrainsWales));
    }

    @Test
    void shouldGetRouteWithHeadsignsAndCorrectServices() {
        String routeId = "17071";
        Route result = transportData.getRouteById(StringIdFor.createId(routeId)); // ariva train man airport to chester

        assertNotNull(result);
        assertEquals("AW train service from MIA to CTR", result.getName());
        assertEquals(ArrivaTrainsWales, result.getAgency());
        assertEquals(routeId,result.getId().forDTO());
        assertTrue(TransportMode.isTrain(result));

        Set<String> headsigns = result.getHeadsigns();
        assertEquals(69, headsigns.size(), "expected headsigns");
    }

    @Test
    void shouldGetTrainRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long walesTrainRoutes = results.stream().filter(route -> route.getAgency().equals(ArrivaTrainsWales)).count();

        assertEquals(256, walesTrainRoutes);
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = TestEnv.LocalNow().plusMonths(48).toLocalDate();
        Set<Service> future = transportData.getServicesOnDate(new TramServiceDate(noTramsDate));
        assertTrue(results.size() > future.size());
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(TrainStations.ManchesterPiccadilly.getId()));
        Station station = transportData.getStationById(TrainStations.ManchesterPiccadilly.getId());
        assertEquals("Manchester Piccadilly", station.getName());

        assertFalse(station.hasPlatforms());
    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataLoaderFiles dataReaderFactory = componentContainer.get(TransportDataLoaderFiles.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0); // yuk
        Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        calendarsDates.close();

        assertFalse(applyToCurrentServices.isEmpty());

        assertEquals(1,  config.getDataSourceConfig().size(), "expected only one data source");
        DataSourceConfig sourceConfig = config.getDataSourceConfig().get(0);
        Set<LocalDate> excludedByConfig = sourceConfig.getNoServices();

        applyToCurrentServices.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            ServiceCalendar calendar = service.getCalendar();

            LocalDate exceptionDate = exception.getDate();
            int exceptionType = exception.getExceptionType();
            if (exceptionType == CalendarDateData.ADDED) {
                if (excludedByConfig.contains(exceptionDate)) {
                    assertFalse(calendar.operatesOn(exceptionDate));
                } else {
                    assertTrue(calendar.operatesOn(exceptionDate));
                }
            } else if (exceptionType == CalendarDateData.REMOVED) {
                assertFalse(calendar.operatesOn(exceptionDate));
            }
        });
    }


}
