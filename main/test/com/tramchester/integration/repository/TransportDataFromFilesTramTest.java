package com.tramchester.integration.repository;


import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.GTFSSourceConfig;
import com.tramchester.dataimport.data.CalendarDateData;
import com.tramchester.dataimport.loader.PopulateTransportDataFromSources;
import com.tramchester.dataimport.loader.TransportDataReader;
import com.tramchester.dataimport.loader.TransportDataReaderFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.LocationType;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.TimeRange;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.integration.testSupport.tram.IntegrationTramTestConfig;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationAvailabilityRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.DataExpiryCategory;
import com.tramchester.testSupport.testTags.DataUpdateTest;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
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

@DataUpdateTest
public class TransportDataFromFilesTramTest {

    // TODO July 2022 +2 for eccles replacement service +10 for august replacement buses
    public static final int NUM_TFGM_TRAM_ROUTES = 14+2+10; // N * since overlaps in data updates
    public static final int NUM_TFGM_TRAM_STATIONS = 99; // summer closures of eccles line
    private static ComponentContainer componentContainer;
    private static IntegrationTramTestConfig config;

    private TransportData transportData;
    private StationAvailabilityRepository availabilityRepository;
    private Collection<Service> allServices;
    private TramRouteHelper routeHelper;

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
        routeHelper = new TramRouteHelper();
    }

    @Test
    void shouldHaveExpectedNumbersForTram() {
        assertEquals(1, transportData.getAgencies().size());
        assertEquals(NUM_TFGM_TRAM_STATIONS, transportData.getStations().size());
        Set<Route> allRoutes = transportData.getRoutes();
        Set<String> uniqueNames = allRoutes.stream().map(Route::getName).collect(Collectors.toSet());
        assertEquals(NUM_TFGM_TRAM_ROUTES, uniqueNames.size(), uniqueNames.toString());

        int expected = 199;
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

    @Test
    void shouldGetRouteWithHeadsigns() {
        Set<Route> results = TestEnv.findTramRoute(transportData, AshtonUnderLyneManchesterEccles);
        results.forEach(result -> {
            assertEquals("Ashton Under Lyne - Manchester - Eccles", result.getName());
            assertEquals(TestEnv.MetAgency(),result.getAgency());
            assertTrue(result.getId().forDTO().startsWith("METLBLUE:I:"));
            assertTrue(TransportMode.isTram(result));
        });
    }

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
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AltrinchamPiccadilly, transportData)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(PiccadillyAltrincham, transportData)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EastDidisburyManchesterShawandCromptonRochdale, transportData)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(RochdaleShawandCromptonManchesterEastDidisbury, transportData)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(VictoriaWythenshaweManchesterAirport, transportData)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(ManchesterAirportWythenshaweVictoria, transportData)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(EcclesManchesterAshtonUnderLyne, transportData)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AshtonUnderLyneManchesterEccles, transportData)));

        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(AltrinchamManchesterBury, transportData)));
        assertTrue(traffordBarRoutes.containsAll(routeHelper.getId(BuryManchesterAltrincham, transportData)));

    }

    @Test
    void shouldHaveExpectedRoutesNonDepot() {
        Set<RouteStation> all = transportData.getRouteStations();

        Set<RouteStation> routeStationSet = all.stream().
                filter(routeStation -> routeStation.getStationId().equals(OldTrafford.getId())).collect(Collectors.toSet());

        Set<Route> callingRoutes = routeStationSet.stream().map(RouteStation::getRoute).collect(Collectors.toSet());

        Set<String> uniqueRouteNames = callingRoutes.stream().map(Route::getName).collect(Collectors.toSet());

        // 6 -> 4 summer 2021
        assertEquals(4, uniqueRouteNames.size());
    }

    @Test
    void shouldGetRouteStationsForStationOnOneRoute() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(Ashton.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(2, routeStationPairs.size(), routeStations.toString());

        Set<String> routeNames =
                routeStations.stream().
                        map(RouteStation::getRoute).
                        map(Route::getName).collect(Collectors.toSet());

        assertEquals(2, routeNames.size(), routeNames.toString());
        assertTrue(routeNames.contains(AshtonUnderLyneManchesterEccles.longName()), routeNames.toString());
        assertTrue(routeNames.contains(EcclesManchesterAshtonUnderLyne.longName()), routeNames.toString());

    }

    @Test
    void extraRouteAtShudehillTowardsEcclesFromVictoria() {
        Route towardsEcclesRoute = transportData.getRouteById(StringIdFor.createId("METLBLUE:I:CURRENT"));
        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(Shudehill)).collect(Collectors.toList());

        List<StopCall> fromVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getFirstStop()).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                collect(Collectors.toList());

        assertEquals(fromVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void extraRouteAtShudehillFromEcclesToVictoria() {
        Route towardsEcclesRoute = transportData.getRouteById(StringIdFor.createId("METLBLUE:O:CURRENT"));
        List<Trip> ecclesTripsViaShudehill = towardsEcclesRoute.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(Shudehill)).collect(Collectors.toList());

        List<StopCall> toVictoria = ecclesTripsViaShudehill.stream().
                map(trip -> trip.getStopCalls().getLastStop()).
                filter(stopCall -> stopCall.getStationId().equals(Victoria.getId())).
                collect(Collectors.toList());

        assertEquals(toVictoria.size(), ecclesTripsViaShudehill.size(), ecclesTripsViaShudehill.toString());
    }

    @Test
    void shouldHaveEndOfLinesExpectedPickupAndDropoffRoutes() {
        Route fromAltrincamToPicc = transportData.getRouteById(StringIdFor.createId("METLPURP:I:CURRENT"));
        Route fromPiccToAltrincham = transportData.getRouteById(StringIdFor.createId("METLPURP:O:CURRENT"));

        Station endOfLine = transportData.getStationById(Altrincham.getId());

        assertFalse(endOfLine.servesRouteDropOff(fromAltrincamToPicc));
        assertTrue(endOfLine.servesRoutePickup(fromAltrincamToPicc));

        assertTrue(endOfLine.servesRouteDropOff(fromPiccToAltrincham));
        assertFalse(endOfLine.servesRoutePickup(fromPiccToAltrincham));

        Station notEndOfLine = transportData.getStationById(NavigationRoad.getId());

        assertTrue(notEndOfLine.servesRouteDropOff(fromAltrincamToPicc));
        assertTrue(notEndOfLine.servesRoutePickup(fromAltrincamToPicc));
        assertTrue(notEndOfLine.servesRouteDropOff(fromPiccToAltrincham));
        assertTrue(notEndOfLine.servesRoutePickup(fromPiccToAltrincham));
    }

    @Test
    void shouldGetRouteStationsForStation() {
        Set<RouteStation> routeStations = transportData.getRouteStationsFor(Shudehill.getId());

        Set<Pair<IdFor<Station>, String>> routeStationPairs = routeStations.stream().
                map(routeStation -> Pair.of(routeStation.getStationId(), routeStation.getRoute().getName())).
                collect(Collectors.toSet());

        assertEquals(8, routeStationPairs.size(), routeStations.toString());

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
        assertTrue(routeNames.contains(AshtonUnderLyneManchesterEccles.longName()));
        assertTrue(routeNames.contains(EcclesManchesterAshtonUnderLyne.longName()));

    }

    @Test
    void shouldGetServicesByDate() {
        LocalDate nextSaturday = TestEnv.nextSaturday();
        Set<Service> results = transportData.getServicesOnDate(nextSaturday);

        assertFalse(results.isEmpty(), "no services next saturday");
        long onCorrectDate = results.stream().
                filter(svc -> svc.getCalendar().operatesOn(nextSaturday)).count();

        assertEquals(results.size(), onCorrectDate, "should all be on the specified date");

        LocalDate noTramsDate = TestEnv.LocalNow().plusMonths(36).toLocalDate(); //transportData.getFeedInfo().validUntil().plusMonths(12);
        results = transportData.getServicesOnDate(noTramsDate);
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
        LocalDate nextSunday = TestEnv.nextSunday();

        Set<Service> sundayServices = transportData.getServicesOnDate(nextSunday);

        Set<Trip> cornbrookTrips = transportData.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(Cornbrook)).collect(Collectors.toSet());

        Set<Trip> sundayTrips = cornbrookTrips.stream().
                filter(trip -> sundayServices.contains(trip.getService())).collect(Collectors.toSet());

        assertFalse(sundayTrips.isEmpty());
    }

    @DataExpiryCategory
    @Test
    void shouldHaveServiceEndDatesBeyondNextNDays() {

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(DAYS_AHEAD);

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
        Set<Service> tramServices = transportData.getServices();

        for (int day = 0; day < DAYS_AHEAD; day++) {
            LocalDate date = TestEnv.testDay().plusDays(day);

            TramServiceDate tramServiceDate = new TramServiceDate(date);
            if (!tramServiceDate.isChristmasPeriod()) {

                Set<Service> servicesOnDate = transportData.getServicesOnDate(date);
                assertFalse(servicesOnDate.isEmpty(), "no services on " + date + " all ids are " + HasId.asIds(tramServices));
            }
        }
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
            if (!tramServiceDate.isChristmasPeriod()) {

                Set<Service> servicesOnDate = transportData.getServicesOnDate(date);

                transportData.getStations().forEach(station -> {
                    Set<Trip> callingTripsOnDate = transportData.getTrips().stream().
                            filter(trip -> trip.getStopCalls().callsAt(station)).
                            filter(trip -> servicesOnDate.contains(trip.getService())).
                            collect(Collectors.toSet());
                    assertFalse(callingTripsOnDate.isEmpty(), String.format("%s %s", date, station.getId()));

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

        Set<Route> routes = transportData.findRoutesByShortName(MutableAgency.METL, AshtonUnderLyneManchesterEccles.shortName());

        assertFalse(routes.isEmpty());

        Set<Trip> toMediaCity = allTrips.stream().
                filter(trip -> trip.getStopCalls().callsAt(Cornbrook)).
                filter(trip -> trip.getStopCalls().callsAt(TramStations.MediaCityUK)).
                filter(trip -> routes.contains(trip.getRoute())).
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
                filter(trip -> trip.departTime().isBefore(time)).
                filter(trip -> trip.arrivalTime().isAfter(time)).
                count();

        assertTrue(onTimeTrips>0);

    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRanges() {
        LocalDate when = TestEnv.testDay();

        // earier to diagnose using end of line station
        Station altrincham = Altrincham.from(transportData);

        long maxDuration = config.getMaxJourneyDuration();

        TimeRange timeRange = TimeRange.of(TramTime.of(12, 50), Duration.ofHours(4), Duration.ofHours(4));
        Set<Route> results = availabilityRepository.getPickupRoutesFor(altrincham, when, timeRange);
        assertEquals(2, results.size(), "for " + timeRange + " missing routes from " + altrincham);
    }

    @DataExpiryCategory
    @Test
    void shouldHaveExpectedRoutesAvailableForDatesAndTimeRangesOverMidnight() {
        LocalDate when = TestEnv.testDay();

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

        LocalDate aMonday = TestEnv.nextMonday();
        assertEquals(DayOfWeek.MONDAY, aMonday.getDayOfWeek());

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
