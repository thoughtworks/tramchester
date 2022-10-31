package com.tramchester.unit.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.*;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.units.indriya.quantity.Quantities;
import tech.units.indriya.unit.Units;

import javax.measure.Quantity;
import javax.measure.quantity.Length;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.Walk;
import static com.tramchester.testSupport.TestEnv.getTrainTestRoute;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static com.tramchester.testSupport.reference.KnownLocations.nearStPetersSquare;
import static org.junit.jupiter.api.Assertions.*;

public class LinkedInterchangeStationTest {

    private IdFor<Station> tramId;
    private IdFor<Station> trainId;

    private MutableStation tramStation;
    private MutableStation trainStation;

    private Route trainPickup;
    private Route trainDropoff;
    private Route tramDropoff;
    private Route tramPickup;

    @BeforeEach
    public void onceBeforeEachTestRuns() {
        tramDropoff = TestEnv.getTramTestRoute(Route.createId("routeTram1"), "tram route 1 name");
        tramPickup = TestEnv.getTramTestRoute(Route.createId("routeTram2"), "tram route 2 name");

        tramId = Station.createId("tramStationId");
        tramStation = new MutableStation(tramId, NaptanArea.createId("naptanId1"),
                "station name", nearStPetersSquare.latLong(), nearStPetersSquare.grid(),  DataSourceID.tfgm);
        tramStation.addRouteDropOff(tramDropoff);
        tramStation.addRoutePickUp(tramPickup);

        trainId = Station.createId("railStationId");
        trainStation = new MutableStation(trainId, NaptanArea.createId("napranId2"),
                "rail stations", nearPiccGardens.latLong(), nearPiccGardens.grid(), DataSourceID.rail);

        trainDropoff = getTrainTestRoute(Route.createId("routeTrain1"), "train route 1 name");
        trainPickup = getTrainTestRoute(Route.createId("routeTrain2"), "train route 2 name");
        trainStation.addRouteDropOff(trainDropoff);
        trainStation.addRoutePickUp(trainPickup);
    }

    @Test
    void shouldHaveCreateLinkedInterchange() {

        Set<TransportMode> modes = Collections.singleton(Walk);
        Quantity<Length> distance = Quantities.getQuantity(200, Units.METRE);
        Duration walkingTime = Duration.ofMinutes(4);

        StationLink tramToTrain = new StationLink(tramStation, trainStation, modes, distance, walkingTime);
        StationLink trainToTram = new StationLink(trainStation, tramStation, modes, distance, walkingTime);

        InterchangeStation tramInterchange = new LinkedInterchangeStation(tramToTrain);

        assertTrue(tramInterchange.isMultiMode());

        assertEquals(tramId, tramInterchange.getStationId());
        assertEquals(tramStation, tramInterchange.getStation());

        Set<Route> pickupRoutes = tramInterchange.getPickupRoutes();
        assertEquals(2, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(tramPickup));
        assertTrue(pickupRoutes.contains(trainPickup));

        assertEquals(1, tramInterchange.getDropoffRoutes().size());
        assertTrue(tramInterchange.getDropoffRoutes().contains(tramDropoff));

        InterchangeStation trainInterchange = new LinkedInterchangeStation(trainToTram);

        assertTrue(trainInterchange.isMultiMode());

        assertEquals(trainId, trainInterchange.getStationId());
        assertEquals(trainStation, trainInterchange.getStation());

        Set<Route> pickupRoutesTrain = trainInterchange.getPickupRoutes();
        assertEquals(2, pickupRoutesTrain.size());
        assertTrue(pickupRoutesTrain.contains(tramPickup));
        assertTrue(pickupRoutesTrain.contains(trainPickup));

        assertEquals(1, trainInterchange.getDropoffRoutes().size());
        assertTrue(trainInterchange.getDropoffRoutes().contains(trainDropoff));
    }

    @Test
    void shouldHaveCreateLinkedInterchangeMultipleLinks() {

        Set<TransportMode> modes = Collections.singleton(Walk);
        Quantity<Length> distance = Quantities.getQuantity(200, Units.METRE);
        Duration walkingTime = Duration.ofMinutes(4);

        Route tramPickupB = TestEnv.getTramTestRoute(Route.createId("routeTram3"), "tram route 3 name");
        Route tramDropoffB = TestEnv.getTramTestRoute(Route.createId("routeTram4"), "tram route 4 name");

        IdFor<Station> tramIdB = Station.createId("tramStationBId");
        MutableStation tramStationB = new MutableStation(tramIdB, NaptanArea.createId("naptanId1"),
                "station B name", nearStPetersSquare.latLong(), nearStPetersSquare.grid(), DataSourceID.tfgm);
        tramStationB.addRoutePickUp(tramPickupB);
        tramStationB.addRouteDropOff(tramDropoffB);

        StationLink trainToTramA = new StationLink(trainStation, tramStation, modes, distance, walkingTime);
        StationLink trainToTramB = new StationLink(trainStation, tramStationB, modes, distance, walkingTime);

        LinkedInterchangeStation trainInterchange = new LinkedInterchangeStation(trainToTramA);
        trainInterchange.addLink(trainToTramB);

        assertEquals(trainId, trainInterchange.getStationId());
        assertEquals(trainStation, trainInterchange.getStation());

        assertEquals(1, trainInterchange.getDropoffRoutes().size());
        assertTrue(trainInterchange.getDropoffRoutes().contains(trainDropoff));

        Set<Route> pickupRoutes = trainInterchange.getPickupRoutes();
        assertEquals(3, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(tramPickup));
        assertTrue(pickupRoutes.contains(tramPickupB));
        assertTrue(pickupRoutes.contains(trainPickup));

    }
}
