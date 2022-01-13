package com.tramchester.integration.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class RailTransportDataFromFilesTest {
    private static ComponentContainer componentContainer;
    private static IntegrationRailTestConfig config;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationRailTestConfig();
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
    void shouldLoadStations() {
        Set<Station> allStations = transportData.getStations();
        assertFalse(allStations.isEmpty());

        BoundingBox bounds = config.getBounds();

        allStations.forEach(station -> assertTrue(bounds.contained(station), station + "out of bounds"));
    }

    @Test
    void shouldHaveCorrectPlatformIds() {
        Station station = transportData.getStationById(Altrincham.getId());
        assertTrue(station.hasPlatforms());

        List<Platform> platforms = new LinkedList<>(station.getPlatforms());

        // should be 2??
        assertEquals(1, platforms.size());

        assertEquals("ALTRNHM:UNK", platforms.get(0).getId().forDTO());
        assertEquals("UNK", platforms.get(0).getPlatformNumber());
    }

    @Test
    void shouldGetSpecificStation() {
        Station result = transportData.getStationById(Derby.getId());

        assertEquals("Derby Rail Station", result.getName());
        assertEquals("Derby", result.getArea());

        final GridPosition expectedGrid = new GridPosition(436182, 335593);
        assertEquals(expectedGrid, result.getGridPosition());

        final LatLong expectedLatLong = CoordinateTransforms.getLatLong(expectedGrid);
        assertEquals(expectedLatLong, result.getLatLong());

        assertEquals(DataSourceID.rail, result.getDataSourceID());
        assertTrue(result.isMarkedInterchange());
    }

    @Test
    void shouldHaveSensibleNames() {
        Station result = transportData.getStationById(ManchesterPiccadilly.getId());

        assertEquals("Manchester Piccadilly Rail Station", result.getName());
        assertEquals("Manchester City Centre", result.getArea());
    }

    @Test
    void shouldHaveExpectedCallingPointsForTripOnARoute() {

        Station piccadilly = ManchesterPiccadilly.getFrom(transportData);
        Station euston = LondonEuston.getFrom(transportData);

        String longName = "VT service from Manchester Piccadilly Rail Station to London Euston Rail Station via Stockport " +
                "Rail Station, Macclesfield Rail Station, Stoke-on-Trent Rail Station, Milton Keynes Central Rail Station";

        List<Station> expectedCallingPoints = Arrays.asList(piccadilly,
                Stockport.getFrom(transportData),
                Macclesfield.getFrom(transportData),
                StokeOnTrent.getFrom(transportData),
                MiltonKeynesCentral.getFrom(transportData),
                euston);

        Set<Route> routes = piccadilly.getPickupRoutes().stream().
                filter(route -> longName.equals(route.getName())).
                collect(Collectors.toSet());

        Set<Trip> wrongCallingPoints = routes.stream().flatMap(route -> route.getTrips().stream()).
                filter(trip -> !trip.getStopCalls().getStationSequence().equals(expectedCallingPoints)).
                collect(Collectors.toSet());

        assertTrue(wrongCallingPoints.isEmpty(), wrongCallingPoints.toString());

    }

    @Test
    void shouldNotLoadTFGMMetStations() {
        final IdFor<Station> unwantedStation = StringIdFor.createId("ALTRMET");

        // TODO Split active vs inactive stations? Problem is don't know modes until after the load
        // likely need to split station load into temp collection first and post filter
        final boolean result = transportData.hasStationId(unwantedStation);
        assertFalse(result);

        Set<RouteStation> routeStations = transportData.getRouteStations();
        IdSet<Station> unwantedRouteStations = routeStations.stream().
                map(routeStation -> routeStation.getStation().getId()).
                filter(unwantedStation::equals).collect(IdSet.idCollector());
        assertTrue(unwantedRouteStations.isEmpty());
    }

    @Test
    void shouldHaveRouteAgencyConsistency() {
        transportData.getAgencies().forEach(agency ->
                agency.getRoutes().forEach(route -> assertEquals(agency, route.getAgency(),
                "Agency wrong for " +route.getId() + " got " + route.getAgency().getId() + " but needed " + agency.getId())));
    }

    @Test
    void shouldHaveNonZeroCostsForStopCallLegs() {

        // was helping to diagnose legit issue, but that has tests elsewhere (RailRouteCostsTest) and is resolved
        // todo likely to break on every data update, remove??

        Set<Trip> allTrips = transportData.getTrips();
        Set<StopCalls> tripWithZeroCostLegs = allTrips.stream().map(Trip::getStopCalls).
                filter(stopCalls -> stopCalls.getLegs().stream().anyMatch(stopLeg -> stopLeg.getCost() == 0)).
                collect(Collectors.toSet());

        assertEquals(55, tripWithZeroCostLegs.size(), tripWithZeroCostLegs.toString());

    }

    @Disabled("now filtering out stations in Ireland etc")
    @Test
    void shouldGetSpecificStationWithoutPosition() {
        // A    KILLARNEY   (CIE              0KILARNYKLL   KLL00000E00000 5

        Station result = transportData.getStationById(StringIdFor.createId("KILARNY"));

        assertEquals("KILLARNEY   (CIE", result.getName());
        assertFalse(result.getGridPosition().isValid());
        assertFalse(result.getLatLong().isValid());
        assertEquals(DataSourceID.rail, result.getDataSourceID());
        assertFalse(result.isMarkedInterchange());
    }

    @Test
    void shouldFindServiceAndTrip() {
        Station startStation = transportData.getStationById(Derby.getId());
        Station endStation = transportData.getStationById(LondonStPancras.getId());

        List<Trip> matchingTrips = transportData.getTrips().stream().
                filter(trip -> trip.getStopCalls().callsAt(startStation)).
                filter(trip -> trip.getStopCalls().callsAt(endStation)).
                filter(trip -> trip.getStopCalls().getStationSequence().get(0).equals(startStation)).
                filter(trip -> trip.getStopCalls().getLastStop().getStation().equals(endStation)).
                collect(Collectors.toList());

        assertFalse(matchingTrips.isEmpty());

        final Trip matchingTrip = matchingTrips.get(0);
        final IdFor<Service> svcId = matchingTrip.getService().getId();
        Service service = transportData.getServiceById(svcId);
        assertNotNull(service);

        Trip trip = transportData.getTripById(matchingTrip.getId());
        assertNotNull(trip);
        assertEquals(service, trip.getService());

        StopCalls stops = trip.getStopCalls();
        final StopCall firstStopCall = stops.getFirstStop();
        assertEquals(startStation, firstStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.None, firstStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.Regular, firstStopCall.getPickupType());

        assertEquals(7, stops.numberOfCallingPoints());

        // 19 is including passed stops, 7 otherwise
        assertEquals(7, stops.totalNumber());

        final StopCall lastStopCall = stops.getLastStop();
        assertEquals(endStation, lastStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.Regular, lastStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.None, lastStopCall.getPickupType());

        // 19 is including passed stops, 7 otherwise
        assertEquals(7, stops.getStationSequence().size());

        // 2 if including passed stop, 0 otherwise
        assertEquals(0, stops.getStationSequence().stream().filter(station -> !station.isActive()).count());

        Route route = trip.getRoute();
        assertNotNull(route);

        RouteStation firstRouteStation = transportData.getRouteStation(startStation, route);
        assertNotNull(firstRouteStation);
    }

    @Test
    void shouldHaveRouteStationConsistency() {

        IdFor<Station> stationId = Wimbledon.getId();
        Station station = transportData.getStationById(stationId);

        Set<Route> pickupRoutes = station.getPickupRoutes();
        assertFalse(pickupRoutes.isEmpty());
        pickupRoutes.forEach(route -> assertNotNull(transportData.getRouteStation(station, route), route.toString()));

        Set<Route> dropoffRoutes = station.getDropoffRoutes();
        assertFalse(dropoffRoutes.isEmpty());
        dropoffRoutes.forEach(route -> assertNotNull(transportData.getRouteStation(station, route), route.toString()));
    }

    @Test
    void shouldHaveDatasourceInfo() {
        Set<DataSourceInfo> infos = transportData.getDataSourceInfo();
        assertFalse(infos.isEmpty());

        List<DataSourceInfo> dataSourceInfos = new ArrayList<>(infos);
        assertEquals(1,dataSourceInfos.size());

        DataSourceInfo info = dataSourceInfos.get(0);
        assertEquals(DataSourceID.rail, info.getID());
        assertEquals(config.getRailConfig().getModes(), info.getModes());

    }

    @Test
    void shouldHaveCorrectPlatforms() {
        IdFor<Station> stationId = LondonWaterloo.getId();
        Station station = transportData.getStationById(stationId);

        IdFor<Platform> platformId = StringIdFor.createId("WATRLMN:12");

        Optional<Platform> result = station.getPlatforms().stream().filter(platform -> platform.getId().equals(platformId)).findFirst();

        assertTrue(result.isPresent());
        final Platform platform12 = result.get();
        assertEquals("12", platform12.getPlatformNumber());
        assertEquals("London Waterloo Rail Station platform 12", platform12.getName());
    }

    @Test
    void shouldMultipleRouteForSameStartEndDependingOnCallingPoints() {
        Set<Route> matchingRoutes = transportData.getTrips().stream().
                filter(trip -> matches(ManchesterPiccadilly.getId(), LondonEuston.getId(), trip)).
                map(Trip::getRoute).
                collect(Collectors.toSet());
        assertNotEquals(1, matchingRoutes.size(), matchingRoutes.toString());
    }

    @Test
    void shouldHaveRouteFromManchesterToLondon() {
        Set<Route> matchingRoutes = transportData.getTrips().stream().
                filter(trip -> matches(ManchesterPiccadilly.getId(), LondonEuston.getId(), trip)).
                map(Trip::getRoute).
                collect(Collectors.toSet());

        assertFalse(matchingRoutes.isEmpty());
        IdSet<Route> routeIds = matchingRoutes.stream().collect(IdSet.collector());
        assertTrue(routeIds.contains(StringIdFor.createId("VT:MNCRPIC=>EUSTON:1")), "did find route " + routeIds);

        Set<Service> matchingServices = matchingRoutes.stream().
                flatMap(route -> route.getServices().stream()).collect(Collectors.toSet());

        assertFalse(matchingServices.isEmpty(), "no services for " + matchingRoutes);

        LocalDate when = TestEnv.testDay();

        Set<Service> runningServices = matchingServices.stream().
                filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(runningServices.isEmpty(), "none running from " + runningServices);

        TramTime time = TramTime.of(8,5);

        Set<Trip> am8Trips = transportData.getTrips().stream().
                filter(trip -> runningServices.contains(trip.getService())).
                filter(trip -> trip.departTime().isBefore(time) && trip.arrivalTime().isAfter(time)).
                collect(Collectors.toSet());

        assertFalse(am8Trips.isEmpty(), "No trip at required time " + runningServices);
    }

    @Test
    void shouldHaveRoutesForAgencyStartAndEnd() {
        IdFor<Agency> agencyId = StringIdFor.createId("VT");
        Optional<Agency> foundAgency = transportData.getAgencies().stream().
                filter(agency -> agency.getId().equals(agencyId)).findFirst();

        IdFor<Station> startId = ManchesterPiccadilly.getId();
        IdFor<Station> endId = LondonEuston.getId();

        assertTrue(foundAgency.isPresent());
        Agency agency = foundAgency.get();

        // all routes for VT between start and end
        Set<MutableRailRoute> routes = transportData.getRoutes().stream().
                filter(route -> route instanceof MutableRailRoute).
                filter(route -> route.getAgency().equals(agency)).
                map(route -> (MutableRailRoute)route).
                filter(route -> route.getBegin().getId().equals(startId)).
                filter(route -> route.getEnd().getId().equals(endId)).
                collect(Collectors.toSet());

        // get unique sets of calling points
        Set<List<Station>> uniqueCallingPoints = routes.stream().
                map(MutableRailRoute::getCallingPoints).collect(Collectors.toSet());

        assertEquals(routes.size(), uniqueCallingPoints.size());

        assertEquals(59, routes.size(), routes.toString());
    }

    @Test
    void shouldHaveAllRoutesWithOperatingDays() {
        Set<Route> noDays = transportData.getRoutes().stream().
                filter(route -> route.getOperatingDays().isEmpty()).
                collect(Collectors.toSet());
        assertTrue(noDays.isEmpty());
    }

    @Test
    void shouldHaveSaneServiceStartAndFinishTimes() {
        Set<Service> allServices = transportData.getServices();
        Set<Service> badTimings = allServices.stream().filter(svc -> svc.getStartTime().isAfter(svc.getFinishTime())).
                collect(Collectors.toSet());
        assertTrue(badTimings.isEmpty());
    }

    @Test
    void shouldNotHaveRailReplacementBusAsTransportModeForRoutesShouldBeTrain() {
       transportData.getRoutes().forEach(route ->
               assertNotEquals(TransportMode.RailReplacementBus, route.getTransportMode(),
                       "route should be rail " +route));
    }

    @Test
    void shouldHaveSomeRailRoutesThatContainTripsWithRailReplacementBusTransportMode() {
        assertTrue(transportData.getRoutes().stream().
                filter(route -> route.getTransportMode()==Train).
                anyMatch(route -> route.getTrips().stream().anyMatch(trip -> trip.getTransportMode()==RailReplacementBus)));
    }

    @Test
    void shouldHaveSensibleTimingsForStoplegs() {
        // was to catch issues with crossing midnight causing wrong timings
        Set<Route> notShips = transportData.getRoutes().stream().
                filter(route -> route.getTransportMode()!=Ship).collect(Collectors.toSet());

        for (Route route : notShips) {
            List<StopCalls.StopLeg> over = route.getTrips().stream().flatMap(trip -> trip.getStopCalls().getLegs().stream()).
                    filter(stopLeg -> stopLeg.getCost() > 12 * 24).
                    collect(Collectors.toList());
            assertTrue(over.isEmpty(), route + " " + over);
        }

    }

    private boolean matches(IdFor<Station> firstId, IdFor<Station> secondId, Trip trip) {
        StopCall firstCall = trip.getStopCalls().getFirstStop();
        if (!firstCall.getStationId().equals(firstId)) {
            return false;
        }
        StopCall finalCall = trip.getStopCalls().getLastStop();
        return secondId.equals(finalCall.getStationId());
    }



}
