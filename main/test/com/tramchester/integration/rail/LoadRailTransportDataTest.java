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
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class LoadRailTransportDataTest {
    private static ComponentContainer componentContainer;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig configuration = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(configuration, TestEnv.NoopRegisterMetrics());
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
    }

    @Test
    void shouldGetSpecificStation() {
        Station result = transportData.getStationById(StringIdFor.createId("DRBY"));

        assertEquals("DERBY", result.getName());
        final GridPosition expectedGrid = new GridPosition(436200, 335600);
        assertEquals(expectedGrid, result.getGridPosition());

        final LatLong expectedLatLong = CoordinateTransforms.getLatLong(expectedGrid);
        assertEquals(expectedLatLong, result.getLatLong());

        assertEquals(DataSourceID.rail, result.getDataSourceID());
        assertTrue(result.isMarkedInterchange());
    }

    @Test
    void shouldHaveRouteAgencyConsistency() {
        transportData.getAgencies().forEach(agency -> {
            agency.getRoutes().forEach(route -> assertEquals(agency, route.getAgency(),
                    "Agency wrong for " +route.getId() + " got " + route.getAgency().getId() + " but needed " + agency.getId()));
        });
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

        final StopCall lastStopCall = stops.getStopBySequenceNumber(stops.numberOfCallingPoints());
        assertEquals(endStation, lastStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.Regular, lastStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.None, lastStopCall.getPickupType());

        Route route = trip.getRoute();
        assertNotNull(route);

        RouteStation firstRouteStation = transportData.getRouteStation(startStation, route);
        assertNotNull(firstRouteStation);
    }

    @Test
    void shouldHaveRouteStationConsistency() {

        IdFor<Station> stationId = Wimbledon.getId(); //StringIdFor.createId("WDON");
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
        assertEquals(Collections.singleton(Train), info.getModes());

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
        assertEquals("LONDON WATERLOO platform 12", platform12.getName());
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
                map(MutableRailRoute::getCallingPoiints).collect(Collectors.toSet());

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

    @Test
    void shouldSpikeServiceTimingDifferences() {
        fail("wip");
//        transportData.getServices().stream().
//                filter(service -> transportData.getTrips().stream().map(trip -> trip.latestDepartTime()))
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
