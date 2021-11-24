package com.tramchester.integration.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.StopCalls;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
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

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
public class LoadTransportDataTest {
    private static ComponentContainer componentContainer;
    private TransportData transportData;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        // TODO rail config?
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
    }

    @Test
    void shouldGetSpecificStationWithoutPosition() {
        // A    KILLARNEY   (CIE              0KILARNYKLL   KLL00000E00000 5

        Station result = transportData.getStationById(StringIdFor.createId("KILARNY"));

        assertEquals("KILLARNEY   (CIE", result.getName());
        assertFalse(result.getGridPosition().isValid());
        assertFalse(result.getLatLong().isValid());
    }

    @Test
    void shouldFindServiceAndTrip() {
        Station startStation = transportData.getStationById(StringIdFor.createId("DRBY"));
        Station endStation = transportData.getStationById(StringIdFor.createId("STPX"));

        Service service = transportData.getServiceById(StringIdFor.createId("G91001:20210517:20211206"));
        assertNotNull(service);

        Trip trip = transportData.getTripById(StringIdFor.createId("trip:G91001:20210517:20211206"));
        assertNotNull(trip);
        assertEquals(service, trip.getService());

        StopCalls stops = trip.getStopCalls();
        final StopCall firstStopCall = stops.getStopBySequenceNumber(1);
        assertEquals(startStation, firstStopCall.getStation());
        assertEquals(GTFSPickupDropoffType.None, firstStopCall.getDropoffType());
        assertEquals(GTFSPickupDropoffType.Regular, firstStopCall.getPickupType());

        assertEquals(21, stops.numberOfCallingPoints());

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

        IdFor<Station> stationId = StringIdFor.createId("WDON");
        Station station = transportData.getStationById(stationId);

        Set<Route> routesFromStaiton = station.getRoutes();

        routesFromStaiton.forEach(route -> assertNotNull(transportData.getRouteStation(station, route), route.toString()));
    }

    @Test
    void shouldHaveCorrectPlatforms() {
        IdFor<Station> stationId = StringIdFor.createId("WATRLMN");
        Station station = transportData.getStationById(stationId);

        IdFor<Platform> platformId = StringIdFor.createId("WATRLMN:12");

        Optional<Platform> result = station.getPlatforms().stream().filter(platform -> platform.getId().equals(platformId)).findFirst();

        assertTrue(result.isPresent());
        final Platform platform12 = result.get();
        assertEquals("12", platform12.getPlatformNumber());
        assertEquals("LONDON WATERLOO platform 12", platform12.getName());

    }

    @Test
    void shouldHaveRouteStation() {

        // MixedCompositeId{Id{'SN:HYWRDSH=>POLGATE'},Id{'HMPDNPK'}}

        IdFor<Route> routeId = StringIdFor.createId("SN:HYWRDSH=>POLGATE");
        IdFor<Station> stationId = StringIdFor.createId("HMPDNPK");

        Route route = transportData.getRouteById(routeId);
        assertNotNull(route);

        Station station = transportData.getStationById(stationId);
        assertNotNull(station);

        Set<RouteStation> routeStations = transportData.getRouteStationsFor(stationId);
        assertFalse(routeStations.isEmpty(), routeStations.toString());

        RouteStation routeStation = transportData.getRouteStationById(RouteStation.createId(stationId, routeId));
        assertNotNull(routeStation, routeStations.toString());
    }

}
