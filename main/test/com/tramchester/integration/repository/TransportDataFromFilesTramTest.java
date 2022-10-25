package com.tramchester.integration.repository;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.loader.PopulateTransportDataFromSources;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.ClosedStationsRepository;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import com.tramchester.testSupport.testTags.PiccGardens2022;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.CentralZoneStation.StPetersSquare;
import static com.tramchester.domain.reference.CentralZoneStation.TraffordBar;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.testSupport.TestEnv.DAYS_AHEAD;
import static com.tramchester.testSupport.TransportDataFilter.getTripsFor;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

// TODO Split out by i/f roles, this has gotten too big
// Begun, started to create tests by repository instead

@DataUpdateTest
public class TransportDataFromFilesTramTest {

    public static final int NUM_TFGM_TRAM_ROUTES = 14; // N * since overlaps in data updates
    public static final int NUM_TFGM_TRAM_STATIONS = 99; // summer closures of eccles line
    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig config;

    private TransportData transportData;
    private StationAvailabilityRepository availabilityRepository;
    private Collection<Service> allServices;
    private TramRouteHelper routeHelper;
    private ClosedStationsRepository closedStationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationTramTestConfig();
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
        availabilityRepository = componentContainer.get(StationAvailabilityRepository.class);
        allServices = transportData.getServices();
        routeHelper = new TramRouteHelper(transportData);
        closedStationRepository = componentContainer.get(ClosedStationsRepository.class);
    }

    @Test
    void shouldHaveExpectedNumbersForTram() {
        assertEquals(1, transportData.getAgencies().size());
        assertEquals(NUM_TFGM_TRAM_STATIONS, transportData.getStations().size());
        Set<Route> allRoutes = transportData.getRoutes();
        Set<String> uniqueNames = allRoutes.stream().map(Route::getName).collect(Collectors.toSet());
        assertEquals(NUM_TFGM_TRAM_ROUTES, uniqueNames.size(), uniqueNames.toString());

        int expected = 198;
        assertEquals(expected, transportData.getPlatforms().size());
    }

    @Test
    void shouldGetFeedInfo() {
        FeedInfo result = transportData.getFeedInfos().get(DataSourceID.tfgm);
        assertEquals("https://www.tfgm.com", result.getPublisherUrl());
    }

    @Test
    void shouldGetAgenciesWithNames() {
        List<Agency> agencies = new ArrayList<>(transportData.getAgencies());
        assertEquals(1, agencies.size()); // just MET for trams
        assertIdEquals("METL", agencies.get(0).getId());
        assertEquals("Metrolink", agencies.get(0).getName());
    }

    @PiccGardens2022
    @Test
    void shouldHaveRouteStationsThatOccurDueToDepot() {
        Set<RouteStation> routeStations = transportData.getRouteStations();

        Set<RouteStation> traffordBar = routeStations.stream().
                filter(routeStation -> routeStation.getStationId().equals(TraffordBar.getId())).collect(Collectors.toSet());

        IdSet<Route> traffordBarRoutes = traffordBar.stream().
                map(RouteStation::getRoute).map(Route::getId).collect(IdSet.idCollector());

        // cannot check for specific size as the way routes handled in tfgm gtfs feed can lead to duplicates
        //assertEquals(10, traffordBarRoutes.size());

        // contains -> containsAll

        // todo PiccGardens2022
        //assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AltrinchamPiccadilly)));
        //assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(PiccadillyAltrincham)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EastDidisburyManchesterShawandCromptonRochdale)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(RochdaleShawandCromptonManchesterEastDidisbury)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(VictoriaWythenshaweManchesterAirport)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(ManchesterAirportWythenshaweVictoria)));

        // todo PiccGardens2022
        //assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EcclesManchesterAshtonUnderLyne)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AshtonUnderLyneManchesterEccles)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AltrinchamManchesterBury)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(BuryManchesterAltrincham)));

    }

    @Test
    void shouldHaveExpectedRoutesNonDepot() {
        Set<RouteStation> all = transportData.getRouteStations();

        Set<RouteStation> routeStationSet = all.stream().
                filter(routeStation -> routeStation.getStationId().equals(OldTrafford.getId())).collect(Collectors.toSet());

        Set<Route> callingRoutes = routeStationSet.stream().map(RouteStation::getRoute).collect(Collectors.toSet());

        Set<String> uniqueRouteNames = callingRoutes.stream().map(Route::getName).collect(Collectors.toSet());

        assertEquals(3, uniqueRouteNames.size());
    }

    @Test
    void shouldGetRouteStationsForStationOnOneRoute() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(ManAirport.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(2, routeStationPairs.size(), routeStationPairs.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertTrue(routeNames.contains(VictoriaWythenshaweManchesterAirport.longName()), routeNames.toString());
        assertTrue(routeNames.contains(ManchesterAirportWythenshaweVictoria.longName()), routeNames.toString());

    }

    @Test
    void shouldHaveRoutesWithStationsAndCallingPoints() {
        Set<Route> allRoutes = transportData.getRoutes();

        Set<Station> allStations = transportData.getStations();

        Set<Route> noPickups = allRoutes.stream().
                filter(route -> allStations.stream().noneMatch(station -> station.servesRoutePickup(route))).
                collect(Collectors.toSet());

        assertTrue(noPickups.isEmpty(), noPickups.toString());

        Set<Route> noDropOffs = allRoutes.stream().
                filter(route -> allStations.stream().noneMatch(station -> station.servesRouteDropOff(route))).
                collect(Collectors.toSet());

        assertTrue(noDropOffs.isEmpty(), noDropOffs.toString());
    }

    @Test
    void shouldHaveExpectedStationsForGreenFromAlty() {
        TramDate when = TestEnv.testDay();
        Route green = routeHelper.getOneRoute(AltrinchamManchesterBury, when);

        Set<Station> allStations = transportData.getStations();

        IdSet<Station> dropOffs = allStations.stream().filter(station -> station.servesRouteDropOff(green)).collect(IdSet.collector());

        assertEquals(24, dropOffs.size(), dropOffs.toString());
        assertFalse(dropOffs.contains(Altrincham.getId()));
        assertTrue(dropOffs.contains(Bury.getId()));
        assertTrue(dropOffs.contains(Cornbrook.getId()));
        assertTrue(dropOffs.contains(Shudehill.getId()));

        IdSet<Station> pickUps = allStations.stream().filter(station -> station.servesRoutePickup(green)).collect(IdSet.collector());

        assertEquals(24, pickUps.size(), pickUps.toString());
        assertTrue(pickUps.contains(Altrincham.getId()));
        assertFalse(pickUps.contains(Bury.getId()));
        assertTrue(pickUps.contains(Cornbrook.getId()));
        assertTrue(pickUps.contains(Shudehill.getId()));

    }

    @Test
    void shouldHaveExpectedStationsForGreenFromBury() {
        TramDate when = TestEnv.testDay();
        Route green = routeHelper.getOneRoute(BuryManchesterAltrincham, when);

        Set<Station> allStations = transportData.getStations();

        IdSet<Station> dropOffs = allStations.stream().filter(station -> station.servesRouteDropOff(green)).collect(IdSet.collector());

        assertEquals(24, dropOffs.size(), dropOffs.toString());
        assertFalse(dropOffs.contains(Bury.getId()));
        assertTrue(dropOffs.contains(Altrincham.getId()));
        assertTrue(dropOffs.contains(Cornbrook.getId()));
        assertTrue(dropOffs.contains(Shudehill.getId()));

        IdSet<Station> pickUps = allStations.stream().filter(station -> station.servesRoutePickup(green)).collect(IdSet.collector());

        assertEquals(24, pickUps.size(), pickUps.toString());
        assertTrue(pickUps.contains(Bury.getId()));
        assertFalse(pickUps.contains(Altrincham.getId()));
        assertTrue(pickUps.contains(Cornbrook.getId()));
        assertTrue(pickUps.contains(Shudehill.getId()));

    }

    @Test
    void shouldGetRouteStationsForStation() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(Shudehill.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(7, routeStationPairs.size(), routeStations.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertTrue(routeNames.contains(VictoriaWythenshaweManchesterAirport.longName()));
        assertTrue(routeNames.contains(ManchesterAirportWythenshaweVictoria.longName()));

        assertTrue(routeNames.contains(BuryPiccadilly.longName()));
        assertTrue(routeNames.contains(PiccadillyBury.longName()));

        assertTrue(routeNames.contains(AltrinchamManchesterBury.longName()));
        assertTrue(routeNames.contains(BuryManchesterAltrincham.longName()));

        // these not on the route map, but some early morning eccles trips seem to start at victoria
        // see extraRouteAtShudehillTowardsEccles above
        //assertTrue(routeNames.contains(AshtonUnderLyneManchesterEccles.longName()));
        assertTrue(routeNames.contains(EcclesManchesterAshtonUnderLyne.longName()));

    }

    @Test
    void shouldGetServicesByDate() {
        TramDate nextSaturday = TestEnv.nextSaturday();
        Set<Service> results = transportData.getServicesOnDate(nextSaturday);

        assertFalse(results.isEmpty(), "no services next saturday");
        long onCorrectDate = results.stream().
                filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();

        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = TestEnv.LocalNow().plusMonths(36).toLocalDate(); //transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(TramDate.of(noTramsDate));
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldHaveSaneServiceStartAndFinishTimes() {
        Set<Service> badTimings = allServices.stream().filter(svc -> svc.getStartTime().isAfter(svc.getFinishTime())).
                collect(Collectors.toSet());
        assertTrue(badTimings.isEmpty());
    }

    @Test
    void shouldHaveSundayServicesFromCornbrook() {
        TramDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(nextSunday);

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(Cornbrook)).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().
                filter(trip -> sundayServices.contains(trip.getService())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {

        TramDate startDate = TramDate.of(LocalDate.now());
        TramDate endDate = startDate.plusDays(DAYS_AHEAD);

        DateRange dateRange = DateRange.of(startDate, endDate);

        Set<Service> services = transportData.getServices();
        Set<Service> expiringServices = services.stream().
                filter(service -> !service.getCalendar().getDateRange().overlapsWith(dateRange)).
                collect(Collectors.toSet());

        assertNotEquals(services, expiringServices, "all services are expiring");
    }

    @Disabled("Solved by removing reboarding filter which does not impact depth first performance")
    @Test
    void shouldCheckTripsFinishingAtNonInterchangeStationsOrEndOfLines() {
        InterchangeRepository interchangeRepository = componentContainer.get(InterchangeRepository.class);
        Set<Trip> allTrips = transportData.getTrips();

        Set<String> endTripNotInterchange = allTrips.stream().
                map(trip -> trip.getStopCalls().getLastStop()).
                map(StopCall::getStation).
                filter(station -> !interchangeRepository.isInterchange(station)).
                filter(station -> !TramStations.isEndOfLine(station)).
                map(Station::getName).
                collect(Collectors.toSet());

        assertTrue(endTripNotInterchange.isEmpty(), "End trip not interchange: " + endTripNotInterchange);
    }

    @Test
    void shouldHandleStopCallsThatCrossMidnight() {
        Set<Route> routes = transportData.getRoutes();

        for (Route route : routes) {
            List<StopCalls.StopLeg> over = route.getTrips().stream().flatMap(trip -> trip.getStopCalls().getLegs(false).stream()).
                    filter(stopLeg -> stopLeg.getCost().compareTo(Duration.ofMinutes(12*24)) > 0).
                    collect(Collectors.toList());
            assertTrue(over.isEmpty(), over.toString());
        }

    }

    @DataExpiryCategory
    @Test
    void shouldHaveTramServicesAvailableNDaysAhead() {

        Set<TramDate> noServices = getUpcomingDates().
                filter(date -> isValidDateToCheck(date)).
                filter(date -> transportData.getServicesOnDate(date).isEmpty()).
                collect(Collectors.toSet());

        assertTrue(noServices.isEmpty(), "no services on " + noServices);

    }

    private boolean isValidDateToCheck(TramDate date) {
        TramServiceDate tramServiceDate = new TramServiceDate(date);
        return !tramServiceDate.isChristmasPeriod();
    }

    @NotNull
    private Stream<TramDate> getUpcomingDates() {
        TramDate testDate = TestEnv.testDay();
        return IntStream.range(0, DAYS_AHEAD).boxed().
                map(testDate::plusDays).sorted();
    }

    @DataExpiryCategory
    @Test
    void shouldHaveTripsOnDateForEachStation() {

        Set<Pair<TramDate, IdFor<Station>>> missing = getUpcomingDates().
                filter(this::isValidDateToCheck).
                flatMap(date -> transportData.getStations().stream().map(station -> Pair.of(date, station))).
                filter(pair -> !closedStationRepository.isClosed(pair.getRight(), pair.getLeft())).
                filter(pair -> transportData.getTripsFor(pair.getRight(), pair.getLeft()).isEmpty()).
                map(pair -> Pair.of(pair.getLeft(), pair.getRight().getId())).
                collect(Collectors.toSet());

        assertTrue(missing.isEmpty(), "Got missing trips for " + missing);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveServicesRunningAtReasonableTimesNDaysAhead() {

        int latestHour = 23;
        int earlistHour = 7;

        int maxwait = 25;

        getUpcomingDates().filter(this::isValidDateToCheck).forEach(date -> {

            transportData.getStations().stream().
                    filter(station -> !closedStationRepository.isClosed(station, date)).
                forEach(station -> {
                    Set<Trip> callingTripsOnDate = transportData.getTripsFor(station, date);
                    assertFalse(callingTripsOnDate.isEmpty(), String.format("no trips calling on %s at %s", date, station.getId()));

                    for (int hour = earlistHour; hour < latestHour; hour++) {
                        TramTime tramTime = TramTime.of(hour, 0);

                        Set<StopCall> calling = new HashSet<>();
                        callingTripsOnDate.forEach(trip -> {
                            Set<StopCall> onTime = trip.getStopCalls().stream().
                                    //filter(stop -> stop.getStation().equals(station)).
                                    filter(stop -> tramTime.plusMinutes(maxwait).
                                            between(stop.getArrivalTime(), stop.getArrivalTime().plusMinutes(maxwait))).
                                    collect(Collectors.toSet());
                            calling.addAll(onTime);
                        });
                        assertFalse(calling.isEmpty(), String.format("Stops %s %s %s %s", date.getDayOfWeek(), date, tramTime, station.getName()));
                    }
            });
        });

        for (int day = 0; day < DAYS_AHEAD; day++) {
            TramDate date = TestEnv.testTramDay().plusDays(day);

            TramServiceDate tramServiceDate = new TramServiceDate(date);
            if (!tramServiceDate.isChristmasPeriod()) {

                Set<Service> servicesOnDate = transportData.getServicesOnDate(date);

                transportData.getStations().stream().filter(station -> !closedStationRepository.isClosed(station, date)).
                        forEach(station -> {
                    Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                            filter(trip -> trip.callsAt(station)).
                            filter(trip -> servicesOnDate.contains(trip.getService())).
                            collect(Collectors.toSet());
                    assertFalse(callingTripsOnDate.isEmpty(), String.format("no trips calling on %s at %s", date, station.getId()));

                    for (int hour = earlistHour; hour < latestHour; hour++) {
                        TramTime tramTime = TramTime.of(hour, 0);

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
    }

    @Test
    void shouldHaveAtLeastOnePlatformForEveryStation() {
        Set<Station> stations = transportData.getStations();
        Set<Station> noPlatforms = stations.stream().filter(station -> station.getPlatforms().isEmpty()).collect(Collectors.toSet());
        assertEquals(Collections.emptySet(),noPlatforms);
    }

    @Test
    void shouldGetStation() {
        assertTrue(transportData.hasStationId(Altrincham.getId()));
        Station station = transportData.getStationById(Altrincham.getId());
        assertEquals("Altrincham", station.getName());

        assertTrue(station.hasPlatforms());

        assertEquals(1, station.getPlatforms().size());
        final Optional<Platform> maybePlatformOne = station.getPlatforms().stream().findFirst();
        assertTrue(maybePlatformOne.isPresent());

        Platform platformOne = maybePlatformOne.get();
        final IdFor<Platform> expectedId = Altrincham.createIdFor("1");

        assertEquals(expectedId, platformOne.getId());
        assertEquals( "1", platformOne.getPlatformNumber());
        assertEquals( "Altrincham platform 1", platformOne.getName());

        // Needs naptan enabled to work
        //assertEquals(station.getAreaId(), platformOne.getAreaId());

        assertEquals(station.getDataSourceID(), platformOne.getDataSourceID());
        assertEquals(LocationType.Platform, platformOne.getLocationType());

        assertEquals(DataSourceID.tfgm, station.getDataSourceID());
    }

    @Test
    @Disabled("naptan load is disabled for trams")
    void shouldHaveAreaForCityCenterStop() {
        Station station = transportData.getStationById(StPetersSquare.getId());
        assertEquals("St Peter's Square", station.getName());
    }

    @Test
    void shouldHavePlatformAndAreaForCityCenter() {
        IdFor<Platform> platformId = StPetersSquare.getPlatformId("3");

        //assertTrue(transportData.hasPlatformId(id));
        Platform platform = transportData.getPlatformById(platformId);
        assertNotNull(platform, "could not find " + platformId);
        assertEquals("St Peter's Square platform 3", platform.getName());
        assertEquals(TramStations.StPetersSquare.createIdFor("3"), platform.getId());
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
        TramDateSet excludedByConfig = TramDateSet.of(sourceConfig.getNoServices());

        applyToCurrentServices.forEach(exception -> {
            Service service = transportData.getServiceById(exception.getServiceId());
            ServiceCalendar calendar = service.getCalendar();

            TramDate exceptionDate = exception.getDate();
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

        Set<Route> routes = transportData.findRoutesByShortName(MutableAgency.METL, AshtonUnderLyneManchesterEccles.shortName());

        assertFalse(routes.isEmpty());

        Set<Trip> toMediaCity = allTrips.stream().
                filter(trip -> trip.callsAt(Cornbrook)).
                filter(trip -> trip.callsAt(TramStations.MediaCityUK)).
                filter(trip -> routes.contains(trip.getRoute())).
                collect(Collectors.toSet());

        Set<Service> services = toMediaCity.stream().
                map(Trip::getService).collect(Collectors.toSet());

        TramDate nextTuesday = TestEnv.testTramDay();

        Set<Service> onDay = services.stream().
                filter(service -> service.getCalendar().operatesOn(nextTuesday)).
                collect(Collectors.toSet());

        assertFalse(onDay.isEmpty());

        TramTime time = TramTime.of(12, 0);

        long onTimeTrips = toMediaCity.stream().
                filter(trip -> trip.departTime().isBefore(time)).
                filter(trip -> trip.arrivalTime().isAfter(time)).
                count();

        assertTrue(onTimeTrips>0);

    }

    @PiccGardens2022
    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRanges() {
        TramDate when = TestEnv.testTramDay();

        // earier to diagnose using end of line station
        Station altrincham = Altrincham.from(transportData);

        TimeRange timeRange = TimeRange.of(TramTime.of(12, 50), Duration.ofHours(4), Duration.ofHours(4));

        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange);

        // 2 -> 1, no piccadilly route
        assertEquals(1, results.size(),
                timeRange + " missing routes from " + altrincham.getId() + " got " + results);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesOverMidnight() {
        TramDate when = TestEnv.testTramDay();

        // earier to diagnose using end of line station
        Station altrincham = Altrincham.from(transportData);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRange = TimeRange.of(TramTime.of(22, 50), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange);
        assertFalse(results.isEmpty(), "for " + timeRange + " missing routes from " + altrincham);

        TimeRange timeRangeCrossMidnight = TimeRange.of(TramTime.of(23, 59), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> overMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeCrossMidnight);
        assertFalse(overMidnightResults.isEmpty(), "for " + timeRangeCrossMidnight + " missing routes over mid-night from " + altrincham);

        TimeRange timeRangeATMidnight = TimeRange.of(TramTime.of(0, 0), Duration.ZERO, Duration.ofMinutes(maxDuration));
        Set<Route> atMidnightResults = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRangeATMidnight);
        assertFalse(atMidnightResults.isEmpty(), "for " + timeRangeATMidnight + " missing routes over mid-night from " + altrincham);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveCorrectDataForTramsCallingAtVeloparkMonday8AM() {
        Set<Trip> origTrips = getTripsFor(transportData.getTrips(), TramStations.VeloPark);

        TramDate aMonday = TestEnv.nextMonday();
        assertEquals(DayOfWeek.MONDAY, aMonday.getDayOfWeek());

        IdSet<Service> mondayServices = allServices.stream()
                .filter(svc -> svc.getCalendar().operatesOn(aMonday))
                .collect(IdSet.collector());

        // reduce the trips to the ones for the right route on the monday by filtering by service ID
        List<Trip> filteredTrips = origTrips.stream().filter(trip -> mondayServices.contains(trip.getService().getId())).
                collect(Collectors.toList());

        assertFalse(filteredTrips.isEmpty());

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
        PopulateTransportDataFromSources transportDataFromFiles = componentContainer.get(PopulateTransportDataFromSources.class);

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
        return trip.getStopCalls().stream().
                filter(stopCall -> stopCall.getStationId().equals(stationId)).
                collect(Collectors.toList());
    }

}
