package com.tramchester.integration.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.repository.TransportData;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.integration.testSupport.Assertions.assertIdEquals;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailTransportDataFromFilesTest {
    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new TramAndTrainGreaterManchesterConfig();
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

        Set<Station> outOfBounds = allStations.stream().
                filter(station -> station.getGridPosition().isValid()).
                filter(Location::isActive).
                filter(station -> !bounds.contained(station)).collect(Collectors.toSet());

        assertTrue(outOfBounds.isEmpty(), HasId.asIds(outOfBounds));

    }

    @Test
    void shouldHaveCorrectPlatformIds() {
        Station station = transportData.getStationById(Altrincham.getId());
        assertTrue(station.hasPlatforms());

        List<Platform> platforms = new LinkedList<>(station.getPlatforms());

        // should be 2??
        assertEquals(1, platforms.size());

        assertIdEquals("ALTRNHM:UNK", platforms.get(0).getId());
        assertEquals("UNK", platforms.get(0).getPlatformNumber());
    }

    @Test
    void shouldHaveSensibleNames() {
        Station result = transportData.getStationById(ManchesterPiccadilly.getId());

        assertEquals("Manchester Piccadilly Rail Station", result.getName());
        assertIdEquals("910GMNCRPIC", result.getAreaId());
    }

    @Test
    void shouldHaveExpectedCallingPointsForTripOnARoute() {

        Station piccadilly = ManchesterPiccadilly.from(transportData);
        Station euston = LondonEuston.from(transportData);

        String longName = "VT service from Manchester Piccadilly Rail Station to London Euston Rail Station via Stockport " +
                "Rail Station, Macclesfield Rail Station, Stoke-on-Trent Rail Station, Milton Keynes Central Rail Station";

        List<Station> expectedCallingPoints = Arrays.asList(piccadilly,
                Stockport.from(transportData),
                Macclesfield.from(transportData),
                StokeOnTrent.from(transportData),
                MiltonKeynesCentral.from(transportData),
                euston);

        Set<Route> routes = piccadilly.getPickupRoutes().stream().
                filter(route -> longName.equals(route.getName())).
                collect(Collectors.toSet());

        Set<Trip> wrongCallingPoints = routes.stream().
                flatMap(route -> route.getTrips().stream()).
                filter(trip -> !trip.getStopCalls().getStationSequence(false).equals(expectedCallingPoints)).
                collect(Collectors.toSet());

        assertTrue(wrongCallingPoints.isEmpty(), wrongCallingPoints.toString());

    }

//    @Test
//    void shouldNotLoadTFGMMetStations() {
//        final IdFor<Station> unwantedStation = StringIdFor.createId("ALTRMET");
//
//        // TODO Split active vs inactive stations? Problem is don't know modes until after the load
//        // likely need to split station load into temp collection first and post filter
//        final boolean result = transportData.hasStationId(unwantedStation);
//        assertFalse(result);
//
//        Set<RouteStation> routeStations = transportData.getRouteStations();
//        IdSet<Station> unwantedRouteStations = routeStations.stream().
//                map(routeStation -> routeStation.getStation().getId()).
//                filter(unwantedStation::equals).collect(IdSet.idCollector());
//        assertTrue(unwantedRouteStations.isEmpty());
//    }

    @Test
    void shouldHaveExpectedAgencies() {
        Set<Agency> results = transportData.getAgencies();

        // 30 + 1 = train + tfgm
        assertEquals(30+1, results.size());

        long count = results.stream().filter(agency -> Agency.IsMetrolink(agency.getId())).count();
        assertEquals(1, count);

        List<IdFor<Agency>> missingTrainOperatingCompanyName = results.stream().
                map(Agency::getId).
                filter(id -> !Agency.IsMetrolink(id)).
                filter(id -> TrainOperatingCompanies.companyNameFor(id).equals(TrainOperatingCompanies.UNKNOWN.getCompanyName())).
                collect(Collectors.toList());

        assertTrue(missingTrainOperatingCompanyName.isEmpty(), missingTrainOperatingCompanyName.toString());

    }

    @Test
    void shouldHaveRouteAgencyConsistency() {
        transportData.getAgencies().forEach(agency ->
                agency.getRoutes().forEach(route -> assertEquals(agency, route.getAgency(),
                "Agency wrong for " +route.getId() + " got " + route.getAgency().getId() + " but needed " + agency.getId())));
    }

    @Test
    void shouldFindServiceAndTrip() {
        Station startStation = transportData.getStationById(ManchesterPiccadilly.getId());
        Station endStation = transportData.getStationById(Mobberley.getId());

        List<Trip> matchingTrips = transportData.getTrips().stream().
                filter(trip -> trip.callsAt(startStation)).
                filter(trip -> trip.callsAt(endStation)).
                filter(trip -> trip.getStopCalls().getStationSequence(false).get(0).equals(startStation)).
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

        final int totalNumberOfCalls = 7;
        final int expectedPassedStops = 7; // calls at all stations

        assertEquals(totalNumberOfCalls, stops.numberOfCallingPoints(),
                "wrong number of stops " + HasId.asIds(stops.getStationSequence(false)));

        assertEquals(expectedPassedStops, stops.totalNumber());

        final StopCall lastStopCall = stops.getLastStop();
        assertEquals(endStation, lastStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.Regular, lastStopCall.getDropoffType());
        // data says otherwise...
        //assertEquals(GTFSPickupDropoffType.None, lastStopCall.getPickupType());

        assertEquals(totalNumberOfCalls, stops.getStationSequence(false).size());
        assertEquals(expectedPassedStops, stops.getStationSequence(true).size());

        assertEquals(0, stops.getStationSequence(false).stream().filter(station -> !station.isActive()).count());
        //assertEquals(2, stops.getStationSequence(true).stream().filter(station -> !station.isActive()).count());

        Route route = trip.getRoute();
        assertNotNull(route);

        RouteStation firstRouteStation = transportData.getRouteStation(startStation, route);
        assertNotNull(firstRouteStation);
    }

    @Test
    void shouldHaveRouteStationConsistency() {

        IdFor<Station> stationId = ManchesterAirport.getId();
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

        Set<DataSourceID> sourceIds = infos.stream().map(DataSourceInfo::getID).collect(Collectors.toSet());
        assertEquals(2, sourceIds.size(), infos.toString());

        assertTrue(sourceIds.contains(DataSourceID.rail));
        assertTrue(sourceIds.contains(DataSourceID.tfgm));
    }

    @Test
    void shouldHaveCorrectModes() {
        Set<TransportMode> transportModes = config.getTransportModes();

        assertEquals(2, transportModes.size(), transportModes.toString());
        assertTrue(transportModes.contains(Tram));
        assertTrue(transportModes.contains(Train));

    }

    @Test
    void shouldHaveCorrectPlatforms() {
        IdFor<Station> stationId = ManchesterPiccadilly.getId();
        Station station = transportData.getStationById(stationId);

        IdFor<Platform> platformId = StringIdFor.createId("MNCRPIC:12");

        Optional<Platform> result = station.getPlatforms().stream().filter(platform -> platform.getId().equals(platformId)).findFirst();

        assertTrue(result.isPresent());
        final Platform platform12 = result.get();
        assertEquals("12", platform12.getPlatformNumber());
        assertEquals("Manchester Piccadilly Rail Station platform 12", platform12.getName());
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
    void shouldHaveRouteFromManchesterToStockport() {
        Set<Route> matchingRoutes = transportData.getTrips().stream().
                filter(trip -> matches(ManchesterPiccadilly.getId(), Stockport.getId(), trip)).
                map(Trip::getRoute).
                collect(Collectors.toSet());

        assertFalse(matchingRoutes.isEmpty());
        IdSet<Route> routeIds = matchingRoutes.stream().collect(IdSet.collector());
        assertTrue(routeIds.contains(StringIdFor.createId("NT:MNCRPIC=>STKP:1")), "did find route NT:MNCRPIC=>STKP:1" + routeIds);
        assertTrue(routeIds.contains(StringIdFor.createId("VT:MNCRPIC=>STKP:1")), "did find route VT:MNCRPIC=>STKP:1" + routeIds);

        Set<Service> matchingServices = matchingRoutes.stream().
                flatMap(route -> route.getServices().stream()).collect(Collectors.toSet());

        assertFalse(matchingServices.isEmpty(), "no services for " + matchingRoutes);

        TramDate when = TestEnv.testTramDay();

        Set<Service> runningServices = matchingServices.stream().
                filter(service -> service.getCalendar().operatesOn(when)).collect(Collectors.toSet());

        assertFalse(runningServices.isEmpty(), "none running from " + runningServices);

        TramTime time = TramTime.of(8,16);

        Set<Trip> am8Trips = transportData.getTrips().stream().
                filter(trip -> runningServices.contains(trip.getService())).
                filter(trip -> trip.departTime().isBefore(time) && trip.arrivalTime().isAfter(time)).
                collect(Collectors.toSet());

        assertFalse(am8Trips.isEmpty(), "No trip at required time " + runningServices);
    }

    @Test
    void shouldHaveRoutesForManchesterAirport() {
        IdFor<Station> startId = ManchesterPiccadilly.getId();
        IdFor<Station> endId = ManchesterAirport.getId();

        // all routes for between piccadilly and the airport
        Set<MutableRailRoute> routes = transportData.getRoutes().stream().
                filter(route -> route instanceof MutableRailRoute).
                map(route -> (MutableRailRoute)route).
                filter(route -> route.getBegin().getId().equals(startId)).
                filter(route -> route.getEnd().getId().equals(endId)).
                collect(Collectors.toSet());

        assertEquals(4, routes.size(), HasId.asIds(routes));

        Set<String> agencies = routes.stream().map(route -> route.getAgency().getName()).collect(Collectors.toSet());

        assertEquals(3, agencies.size(), agencies.toString());
        assertTrue(agencies.contains(TrainOperatingCompanies.TP.getCompanyName()));
        assertTrue(agencies.contains(TrainOperatingCompanies.NT.getCompanyName()));
        assertTrue(agencies.contains(TrainOperatingCompanies.AW.getCompanyName()));
    }

    @Test
    void shouldHaveAllRoutesWithOperatingDays() {
        Set<Route> noDays = transportData.getRoutes().stream().
                filter(route -> route.getTransportMode().equals(Train)).
                filter(route -> route.getOperatingDays().isEmpty()).
                collect(Collectors.toSet());
        assertTrue(noDays.isEmpty(), HasId.asIds(noDays));
    }

    @Test
    void shouldHaveSaneServiceStartAndFinishTimes() {
        Set<Service> allServices = transportData.getServices();

        Set<Service> badTimings = allServices.stream().
                filter(svc -> svc.getFinishTime().isBefore(svc.getStartTime())).
                collect(Collectors.toSet());

        String diagnostics = badTimings.stream().
                map(service -> service.getId() + " begin: " + service.getStartTime() + " end: " + service.getFinishTime() + " ").
                collect(Collectors.joining());

        assertTrue(badTimings.isEmpty(), diagnostics);
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

    private boolean matches(IdFor<Station> firstId, IdFor<Station> secondId, Trip trip) {
        StopCall firstCall = trip.getStopCalls().getFirstStop();
        if (!firstCall.getStationId().equals(firstId)) {
            return false;
        }
        StopCall finalCall = trip.getStopCalls().getLastStop();
        return secondId.equals(finalCall.getStationId());
    }



}
