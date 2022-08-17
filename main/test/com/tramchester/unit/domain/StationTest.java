package com.tramchester.unit.domain;


import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.StationHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static com.tramchester.domain.MutableAgency.Walking;
import static com.tramchester.domain.reference.TransportMode.*;
import static com.tramchester.testSupport.reference.KnownLocations.nearPiccGardens;
import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

class StationTest {

    private final IdFor<NaptanArea> areaId = StringIdFor.createId("area");

    @Test
    void testShouldCreateCorrecly() {
        Station tramStation = StationHelper.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), DataSourceID.tfgm);

        assertEquals("stopName", tramStation.getName());
        assertEquals(StringIdFor.createId("id"), tramStation.getId());
        assertEquals(-2.0, tramStation.getLatLong().getLat(),0);
        assertEquals(2.3, tramStation.getLatLong().getLon(),0);
        assertEquals(areaId, tramStation.getAreaId());
        assertEquals(DataSourceID.tfgm, tramStation.getDataSourceID());
    }

    @Test
    void testShouldSetBusNameCorrecly() {
        Station busStation = StationHelper.forTest("id", "area", "stopName",
                new LatLong(-2.0, 2.3), DataSourceID.tfgm);

        assertEquals("stopName", busStation.getName());
        assertEquals(StringIdFor.createId("id"), busStation.getId());
        assertEquals(-2.0, busStation.getLatLong().getLat(),0);
        assertEquals(2.3, busStation.getLatLong().getLon(),0);
        assertEquals(areaId, busStation.getAreaId());
        //assertTrue(TransportMode.isBus(busStation));
    }

    @Test
    void shouldHaveCorrectTransportModes() {

        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), areaId, "name", nearPiccGardens.latLong(),
                nearPiccGardens.grid(), DataSourceID.tfgm);

        assertTrue(station.getTransportModes().isEmpty());

        final Route route = MutableRoute.getRoute(StringIdFor.createId("routeIdA"), "shortName", "name",
                TestEnv.MetAgency(), Tram);
        station.addRouteDropOff(route);
        assertTrue(station.servesMode(Tram));

        station.addRouteDropOff(MutableRoute.getRoute(StringIdFor.createId("routeIdB"), "trainShort", "train",
                Walking, Train));
        assertTrue(station.servesMode(Train));

        assertEquals(2, station.getTransportModes().size());
    }

    @Test
    void shouldHavePickupAndDropoffRoutes() {
        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), areaId, "name", nearPiccGardens.latLong(),
                nearPiccGardens.grid(), DataSourceID.tfgm);

        final Route routeA = MutableRoute.getRoute(StringIdFor.createId("routeIdA"), "shortNameA", "nameA",
                TestEnv.MetAgency(), Tram);
        final Route routeB = MutableRoute.getRoute(StringIdFor.createId("routeIdB"), "shortNameB", "nameB",
                TestEnv.StagecoachManchester, Bus);

        assertFalse(station.hasPickup());
        assertFalse(station.hasDropoff());

        station.addRoutePickUp(routeA);
        assertTrue(station.hasPickup());

        station.addRouteDropOff(routeB);
        assertTrue(station.hasDropoff());

        assertTrue(station.servesMode(Tram));
        assertTrue(station.servesMode(Bus));

        Set<Agency> agencies = station.getAgencies();
        assertEquals(2, agencies.size());
        assertTrue(agencies.contains(TestEnv.MetAgency()));
        assertTrue(agencies.contains(TestEnv.StagecoachManchester));

        Set<Route> dropOffRoutes = station.getDropoffRoutes();
        assertEquals(1, dropOffRoutes.size());
        assertTrue(dropOffRoutes.contains(routeB));

        Set<Route> pickupRoutes = station.getPickupRoutes();
        assertEquals(1, pickupRoutes.size());
        assertTrue(pickupRoutes.contains(routeA));

        assertTrue(station.servesRoutePickup(routeA));
        assertFalse(station.servesRoutePickup(routeB));

        assertTrue(station.servesRouteDropOff(routeB));
        assertFalse(station.servesRouteDropOff(routeA));

        // TODO Routes for platforms?
    }

    @Test
    void shouldHavePickupAndDropoffRoutesForSpecificDates() {
        EnumSet<DayOfWeek> days = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);

        MutableStation station = new MutableStation(StringIdFor.createId("stationId"), areaId, "name", nearPiccGardens.latLong(),
                nearPiccGardens.grid(), DataSourceID.tfgm);

        final MutableRoute routeA = new MutableRoute(StringIdFor.createId("routeIdA"), "shortNameA", "nameA", TestEnv.MetAgency(), Tram);

        DateRange dateRangeA = new DateRange(LocalDate.of(2022, 7, 15), LocalDate.of(2022, 8, 24));

        MutableService serviceA = createService(days, dateRangeA, "serviceAId");

        DateRange dateRangeB = new DateRange(LocalDate.of(2022, 7, 16), LocalDate.of(2022,7,17));

        MutableService serviceB = createService(days, dateRangeB, "serviceBId");

        routeA.addService(serviceA);
        routeA.addService(serviceB);

        // only add routeA for serviceB , so should respect serviceB dates over the routes range

        station.addRoutePickUp(routeA);


        assertTrue(station.hasPickup());

        assertTrue(station.servesRoutePickup(routeA));
    }

    @NotNull
    private MutableService createService(EnumSet<DayOfWeek> days, DateRange dateRange, String serviceId) {
        MutableService service = new MutableService(StringIdFor.createId(serviceId));
        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(dateRange, days);
        service.setCalendar(serviceCalendar);
        return service;
    }




}
