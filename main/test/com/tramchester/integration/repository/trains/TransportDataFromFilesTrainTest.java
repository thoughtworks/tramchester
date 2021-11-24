package com.tramchester.integration.repository.trains;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.integration.testSupport.train.IntegrationTrainTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TrainStations;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.testSupport.TestEnv.ArrivaTrainsWales;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class TransportDataFromFilesTrainTest {

    public static final int GB_RAIL_AGENCIES = 30;
    public static final int GB_RAIL_NUM_STATIONS = 2652;
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;

    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTrainTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        transportData = componentContainer.get(TransportData.class);
    }

    @Test
    void shouldHaveCheckForStations() {
        assertEquals(GB_RAIL_NUM_STATIONS,transportData.getStations().size());

        // no platforms represented in train data
        assertEquals(0, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetAgencies() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(GB_RAIL_AGENCIES, agencies.size());
        assertTrue(agencies.contains(ArrivaTrainsWales));
    }

    @Test
    void shouldGetRouteWithHeadsignsAndCorrectServices() {
        Route result = TestEnv.singleRoute(transportData, StringIdFor.createId("AW"), "AW:MIA->CTR");

        assertNotNull(result);
        assertEquals("Arriva Trains Wales train service from Manchester Airport to Chester", result.getName());
        assertEquals(ArrivaTrainsWales, result.getAgency());
       //assertEquals(routeId,result.getId().forDTO()); changes too frequently
        assertEquals(TransportMode.Train, result.getTransportMode());
    }

    @Test
    void shouldGetTrainRoutes() {
        assertEquals(3665, transportData.getRoutes().size());

        Collection<Route> results = transportData.getRoutes();
        long walesTrainRoutes = results.stream().filter(route -> route.getAgency().equals(ArrivaTrainsWales)).count();

        assertEquals(251, walesTrainRoutes);
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
    void shouldGetStations() {
        assertTrue(transportData.hasStationId(TrainStations.ManchesterPiccadilly.getId()));
        Station manPicc = transportData.getStationById(TrainStations.ManchesterPiccadilly.getId());
        assertEquals("Manchester Piccadilly", manPicc.getName());

        assertFalse(manPicc.hasPlatforms());

        Station derby = transportData.getStationById(StringIdFor.createId("DBY"));
        assertNotNull(derby);
        assertEquals("Derby", derby.getName());
        assertEquals("", derby.getArea());

        Station edinburgh = transportData.getStationById(StringIdFor.createId("EDB"));
        assertNotNull(edinburgh);
        assertEquals("Edinburgh", edinburgh.getName());

        Station cardiff = transportData.getStationById(StringIdFor.createId("CDF"));
        assertNotNull(cardiff);
        assertEquals("Cardiff Central", cardiff.getName());

    }

    @Test
    void shouldBeApplyingExceptionalDatesCorrectly() {

        TransportDataReaderFactory dataReaderFactory = componentContainer.get(TransportDataReaderFactory.class);
        List<TransportDataReader> transportDataReaders = dataReaderFactory.getReaders();
        TransportDataReader transportDataReader = transportDataReaders.get(0); // yuk
        Stream<CalendarDateData> calendarsDates = transportDataReader.getCalendarDates();

        Set<CalendarDateData> applyToCurrentServices = calendarsDates.
                filter(calendarDateData -> transportData.hasServiceId(calendarDateData.getServiceId())).
                collect(Collectors.toSet());

        calendarsDates.close();

        assertFalse(applyToCurrentServices.isEmpty());

        assertEquals(1,  config.getGTFSDataSource().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGTFSDataSource().get(0);
        Set<LocalDate> excludedByConfig = sourceConfig.getNoServices();

        applyToCurrentServices.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            MutableServiceCalendar calendar = service.getCalendar();

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
