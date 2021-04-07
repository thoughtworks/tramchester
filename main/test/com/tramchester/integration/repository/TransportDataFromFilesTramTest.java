package com.tramchester.integration.repository;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.TransportDataLoaderFiles;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.testSupport.DataExpiryCategory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.CentralZoneStation.TraffordBar;
import static com.tramchester.domain.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.TramStations.Cornbrook;
import static org.junit.jupiter.api.Assertions.*;

class TransportDataFromFilesTramTest {

    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig config;

    private TransportData transportData;
    private Collection<Service> allServices;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
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
    void shouldHaveExpectedNumbersForTram() {
        assertEquals(1, transportData.getAgencies().size());
        assertEquals(99,transportData.getStations().size());
        assertEquals(12, transportData.getRoutes().size());

        // deansgate-castlefield platform 2 (9400ZZMAGMX2) seems to missing from data as of 12/8/2020
        assertEquals(198, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetFeedInfo() {
        FeedInfo result = transportData.getFeedInfos().get(DataSourceID.TFGM());
        assertEquals("https://www.tfgm.com", result.getPublisherUrl());
    }

    @Test
    void shouldGetAgenciesWithNames() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(1, agencies.size()); // just MET for trams
        assertEquals("METL", agencies.get(0).getId().forDTO());
        assertEquals("Metrolink", agencies.get(0).getName());
    }

    @Test
    void shouldGetRouteWithHeadsigns() {
        Route result = transportData.getRouteById(AshtonUnderLyneManchesterEccles.getId());
        assertEquals("Ashton Under Lyne - Manchester - Eccles", result.getName());
        assertEquals(TestEnv.MetAgency(),result.getAgency());
        assertEquals("METLBLUE:I:",result.getId().forDTO());
        assertTrue(TransportMode.isTram(result));

        Set<String> headsigns = result.getHeadsigns();
        assertEquals(2, headsigns.size(), "expected headsigns");
        assertTrue(headsigns.contains("Eccles"));
        assertTrue(headsigns.contains("Trafford Bar"));
    }

    @Test
    void shouldHaveRouteStationsThatOccurDueToDepot() {
        Set<RouteStation> routeStations = transportData.getRouteStations();

        Set<RouteStation> traffordBar = routeStations.stream().
                filter(routeStation -> routeStation.getStationId().equals(TraffordBar.getId())).collect(Collectors.toSet());

        IdSet<Route> traffordBarRoutes = traffordBar.stream().map(RouteStation::getRoute).map(Route::getId).collect(IdSet.idCollector());

        // 2*3 expected, but includes eccles as well
        assertEquals(8, traffordBarRoutes.size());
        assertTrue(traffordBarRoutes.contains(AltrinchamPiccadilly.getId()));
        assertTrue(traffordBarRoutes.contains(PiccadillyAltrincham.getId()));
        assertTrue(traffordBarRoutes.contains(EastDidisburyManchesterShawandCromptonRochdale.getId()));
        assertTrue(traffordBarRoutes.contains(RochdaleShawandCromptonManchesterEastDidisbury.getId()));
        assertTrue(traffordBarRoutes.contains(VictoriaWythenshaweManchesterAirport.getId()));
        assertTrue(traffordBarRoutes.contains(ManchesterAirportWythenshaweVictoria.getId()));

        assertTrue(traffordBarRoutes.contains(EcclesManchesterAshtonUnderLyne.getId()));
        assertTrue(traffordBarRoutes.contains(AshtonUnderLyneManchesterEccles.getId()));
    }

    @Test
    void shouldHaveExpectedRoutesNonDepot() {
        Set<RouteStation> all = transportData.getRouteStations();

        Set<RouteStation> routeStationSet = all.stream().
                filter(routeStation -> routeStation.getStationId().equals(TramStations.OldTrafford.getId())).collect(Collectors.toSet());

        Set<Route> callingRoutes = routeStationSet.stream().map(RouteStation::getRoute).collect(Collectors.toSet());

        assertEquals(2, callingRoutes.size());
    }

    @Test
    void shouldGetTramRoutes() {
        Collection<Route> results = transportData.getRoutes();
        long tramRoutes = results.stream().filter(route -> route.getAgency().equals(TestEnv.MetAgency())).count();

        // todo lockdown 14->12
        assertEquals(12, tramRoutes);
    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        TramServiceDate date = new TramServiceDate(nextSaturday);
        Set<Service> results = transportData.getServicesOnDate(date);

        assertFalse(results.isEmpty());
        long onCorrectDate = results.stream().filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();
        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = TestEnv.LocalNow().plusMonths(36).toLocalDate(); //transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(new TramServiceDate(noTramsDate));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHaveCorrectDatesAndServicesForChirstmas2020() {
        LocalDate christmasDay = LocalDate.of(2020, 12, 25);

        Set<Service> services = transportData.getServicesOnDate(TramServiceDate.of(christmasDay));

        assertTrue(services.isEmpty());
    }

    @Test
    void shouldHaveSundayServicesFromCornbrook() {
        LocalDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(new TramServiceDate(nextSunday));

        IdSet<Service> sundayServiceIds = sundayServices.stream().collect(IdSet.collector());

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(Cornbrook)).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().filter(trip -> sundayServiceIds.
                contains(trip.getService().getId())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @Disabled("Only useful when issues arise with expiry")
    @DataExpiryCategory
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {
        LocalDate queryDate = LocalDate.now().plusDays(DAYS_AHEAD);

        Collection<Service> services = transportData.getServices();
        Set<Service> expiringServices = services.stream().
                filter(svc -> !svc.getCalendar().operatesOn(queryDate)).collect(Collectors.toSet());

        assertEquals(Collections.emptySet(), expiringServices, "Expiring svcs " +HasId.asIds(expiringServices));
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesRunningAtReasonableTimesNDaysAhead() {

        // temporary 23 -> 22, 6->7
        int latestHour = 22;
        int earlistHour = 7;

        int maxwait = 25;

        for (int day = 0; day < DAYS_AHEAD; day++) {
            LocalDate date = TestEnv.testDay().plusDays(day);
            TramServiceDate tramServiceDate = new TramServiceDate(date);
            Set<Service> servicesOnDate = transportData.getServicesOnDate(tramServiceDate);

            IdSet<Service> servicesOnDateIds = servicesOnDate.stream().collect(IdSet.collector());

            transportData.getStations().forEach(station -> {
                Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                        filter(trip -> trip.getStopCalls().callsAt(station)).
                        filter(trip -> servicesOnDateIds.contains(trip.getService().getId())).
                        collect(Collectors.toSet());
                assertFalse(callingTripsOnDate.isEmpty(), String.format("%s %s", date, station));

                for (int hour = earlistHour; hour < latestHour; hour++) {
                    TramTime tramTime = TramTime.of(hour,0);

                    Set<StopCall> calling = new HashSet<>();
                    callingTripsOnDate.forEach(trip -> {
                        Set<StopCall> onTime = trip.getStopCalls().stream().
                                filter(stop -> stop.getStation().equals(station)).
                                filter(stop -> tramTime.plusMinutes(maxwait).
                                        between(stop.getArrivalTime(), stop.getArrivalTime().plusMinutes(maxwait))).
                                collect(Collectors.toSet());
                        calling.addAll(onTime);
                    });
                    assertFalse(calling.isEmpty(), String.format("Stops %s %s %s %s", date.getDayOfWeek(), date, tramTime, station.getName()));
                }
            });
        }
    }

    @Test
    void shouldHaveAtLeastOnePlatformForEveryStation() {
        Set<Station> stations = transportData.getStations();
        Set<Station> noPlatforms = stations.stream().filter(station -> station.getPlatforms().isEmpty()).collect(Collectors.toSet());
        assertEquals(Collections.emptySet(),noPlatforms);
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(TramStations.Altrincham.getId()));
        Station station = transportData.getStationById(TramStations.Altrincham.getId());
        assertEquals("Altrincham", station.getName());

        assertTrue(station.hasPlatforms());
        // only one platform at alty, well according to the timetable anyway....
        assertEquals(1, station.getPlatforms().size());
        Platform platformOne = station.getPlatforms().stream().findFirst().get();
        assertEquals( TramStations.Altrincham.forDTO()+"1", platformOne.getId().forDTO());
        assertEquals( "1", platformOne.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platformOne.getName());
    }

    @Test
    void shouldHavePlatform() {
        StringIdFor<Platform> id = StringIdFor.createId(TramStations.StPetersSquare.forDTO() + "3");

        assertTrue(transportData.hasPlatformId(id));
        Platform platform = transportData.getPlatform(id);
        assertEquals("St Peter's Square platform 3", platform.getName());
        assertEquals(TramStations.StPetersSquare.forDTO()+"3", platform.getId().forDTO());
    }

    @Test
    void shouldHaveAllEndOfLineTramStations() {

        // Makes sure none are missing from the data
        List<Station> filteredStations = transportData.getStations().stream()
                .filter(TramStations::isEndOfLine).collect(Collectors.toList());

        assertEquals(TramStations.EndOfTheLine.size(), filteredStations.size());
    }

    @Test
    void shouldHaveConsistencyOfRouteAndTripAndServiceIds() {
        Collection<Route> allRoutes = transportData.getRoutes();

        Set<Service> uniqueSvcs = allRoutes.stream().map(Route::getServices).flatMap(Collection::stream).collect(Collectors.toSet());

        assertEquals(uniqueSvcs.size(), allServices.size());

        Set<Station> allsStations = transportData.getStations();

        Set<Trip> allTrips = new HashSet<>();
        allsStations.forEach(station -> allTrips.addAll(getTripsFor(transportData.getTrips(), station)));

        int tripsSize = transportData.getTrips().size();
        assertEquals(tripsSize, allTrips.size());

        IdSet<Trip> tripIdsFromSvcs = transportData.getRoutes().stream().map(Route::getTrips).
                flatMap(Collection::stream).
                map(Trip::getId).collect(IdSet.idCollector());
        assertEquals(tripsSize, tripIdsFromSvcs.size());

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

        assertEquals(1,  config.getGTFSDataSource().size(), "expected only one data source");
        GTFSSourceConfig sourceConfig = config.getGTFSDataSource().get(0);
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

    @Test
    void shouldReproIssueAtMediaCityWithBranchAtCornbrook() {
        Set<Trip> allTrips = getTripsFor(transportData.getTrips(), Cornbrook);

        Set<Trip> toMediaCity = allTrips.stream().
                filter(trip -> trip.getStopCalls().callsAt(Cornbrook)).
                filter(trip -> trip.getStopCalls().callsAt(TramStations.MediaCityUK)).
                filter(trip -> trip.getRoute().getId().equals(AshtonUnderLyneManchesterEccles.getId())).
                collect(Collectors.toSet());

        Set<Service> services = toMediaCity.stream().
                map(Trip::getService).collect(Collectors.toSet());

        LocalDate nextTuesday = TestEnv.testDay();

        Set<Service> onDay = services.stream().
                filter(service -> service.getCalendar().operatesOn(nextTuesday)).
                collect(Collectors.toSet());

        assertFalse(onDay.isEmpty());

        TramTime time = TramTime.of(12, 0);

        long onTimeTrips = toMediaCity.stream().
                filter(trip -> trip.earliestDepartTime().isBefore(time)).
                filter(trip -> trip.latestDepartTime().isAfter(time)).
                count();

        assertTrue(onTimeTrips>0);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = getTripsFor(transportData.getTrips(), TramStations.VeloPark);

        LocalDate aMonday = TestEnv.nextMonday();
        assertEquals(DayOfWeek.MONDAY, aMonday.getDayOfWeek());

        // TODO Due to exception dates makes no sense to use getDays
        IdSet<Service> mondayServices = allServices.stream()
                .filter(svc -> svc.getCalendar().operatesOn(aMonday))
                .collect(IdSet.collector());

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        List<Trip> filteredTrips = origTrips.stream().filter(trip -> mondayServices.contains(trip.getService().getId())).
                collect(Collectors.toList());

        assertTrue(filteredTrips.size()>0);

        // find the stops, invariant is now that each trip ought to contain a velopark stop
        List<StopCall> stoppingAtVelopark = filteredTrips.stream()
                .filter(trip -> mondayServices.contains(trip.getService().getId()))
                .map(trip -> getStopsFor(trip, TramStations.VeloPark.getId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        assertEquals(filteredTrips.size(), stoppingAtVelopark.size());

        // finally check there are trams stopping within 15 mins of 8AM on Monday
        stoppingAtVelopark.removeIf(stop -> {
            TramTime arrivalTime = stop.getArrivalTime();
            return arrivalTime.asLocalTime().isAfter(LocalTime.of(7,59)) &&
                    arrivalTime.asLocalTime().isBefore(LocalTime.of(8,16));
        });

        assertTrue(stoppingAtVelopark.size()>=1); // at least 1
        assertNotEquals(filteredTrips.size(), stoppingAtVelopark.size());
    }

    @Disabled("Performance tests")
    @Test
    void shouldLoadData() {
        TransportDataFromFiles transportDataFromFiles = componentContainer.get(TransportDataFromFiles.class);

        int count = 10;
        //int count = 1;
        long total = 0;
        for (int i = 0; i < count; i++) {
            long begin = System.currentTimeMillis();
            //TransportDataFromFiles fromFiles = builder.create();

            transportDataFromFiles.getData();
            long finish = System.currentTimeMillis();

            total = total + (finish - begin);
        }

        System.out.printf("Total: %s ms Average: %s ms%n", total, total/count);
    }

    private List<StopCall> getStopsFor(Trip trip, IdFor<Station> stationId) {
        return trip.getStopCalls().stream().filter(stopCall -> stopCall.getStationId().equals(stationId)).collect(Collectors.toList());
    }

}
