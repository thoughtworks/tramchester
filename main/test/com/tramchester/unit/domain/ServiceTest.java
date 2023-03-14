package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.dates.DateRange;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownLocations.nearShudehill;
import static com.tramchester.testSupport.reference.KnownLocations.nearWythenshaweHosp;
import static com.tramchester.testSupport.reference.TramStations.ManAirport;
import static com.tramchester.testSupport.reference.TramStations.Shudehill;
import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    private final DataSourceID dataSourceId = DataSourceID.tfgm;
    private final IdFor<NaptanArea> areaId = NaptanArea.invalidId();
    private final boolean isMarkedInterchange = false;
    private Station manAirport;
    private Station shudehill;

    @BeforeEach
    void setUp() {
        manAirport = ManAirport.fake();
        shudehill = Shudehill.fake();
    }

    @Test
    void shouldNoticeNoDatesSet() {
        MutableService service = new MutableService(Service.createId("svcXXX"));
        assertFalse(service.hasCalendar());

        TramDate startDate = TramDate.of(2014, 10, 5);
        TramDate endDate = TramDate.of(2014, 12, 25);

        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        service.setCalendar(serviceCalendar);

        assertTrue(service.hasCalendar());

    }

    @Test
    void shouldHaveTimeRange() {
        Platform platformA = new MutablePlatform(Platform.createId(manAirport,"platA"), manAirport, "man airport",
                dataSourceId, "1", areaId, nearWythenshaweHosp.latLong(), nearWythenshaweHosp.grid(), isMarkedInterchange);
        Platform platformB = new MutablePlatform(Platform.createId(shudehill,"platB"), shudehill, "shudehill",
                dataSourceId, "2", areaId, nearShudehill.latLong(), nearShudehill.grid(), isMarkedInterchange);

        Route route = TestEnv.getTramTestRoute();
        MutableService service = new MutableService(Service.createId("svcA"));

        final MutableTrip tripA = new MutableTrip(Trip.createId("tripA"), "headSignA", service, route, Tram);

        final PlatformStopCall platformStopCallA = new PlatformStopCall(platformA, manAirport,
                TramTime.of(8,15), TramTime.of(8,16), 1, Regular, Regular, tripA);
        tripA.addStop(platformStopCallA);

        final MutableTrip tripB = new MutableTrip(Trip.createId("tripB"), "headSignB", service, route, Tram);

        final PlatformStopCall platformStopCallB = new PlatformStopCall(platformB, shudehill,
                TramTime.of(16,25), TramTime.of(16,26), 1, Regular, Regular, tripB);
        tripB.addStop(platformStopCallB);

        service.addTrip(tripA);
        service.addTrip(tripB);

        assertEquals(TramTime.of(8,16), service.getStartTime());
        assertEquals(TramTime.of(16,25), service.getFinishTime());

    }

    @Test
    void shouldHaveTimeRangeCrossesMidnight() {
        Platform platformA = new MutablePlatform(Platform.createId(manAirport,"platA"), manAirport, "man airport",
                dataSourceId, "1", areaId, nearWythenshaweHosp.latLong(), nearWythenshaweHosp.grid(), isMarkedInterchange);
        Platform platformB = new MutablePlatform(Platform.createId(shudehill,"platB"), shudehill, "shudehill",
                dataSourceId, "2", areaId, nearShudehill.latLong(), nearShudehill.grid(), isMarkedInterchange);

        Route route = TestEnv.getTramTestRoute();
        MutableService service = new MutableService(Service.createId("svcA"));

        final MutableTrip tripA = new MutableTrip(Trip.createId("tripA"), "headSignA", service, route, Tram);

        final PlatformStopCall platformStopCallA = new PlatformStopCall(platformA, manAirport,
                TramTime.of(8,15), TramTime.of(8,16), 1, Regular, Regular, tripA);

        tripA.addStop(platformStopCallA);

        final MutableTrip tripB = new MutableTrip(Trip.createId("tripB"), "headSignB", service, route, Tram);

        final PlatformStopCall platformStopCallB = new PlatformStopCall(platformB, shudehill,
                TramTime.nextDay(0,10), TramTime.nextDay(0,15), 1, Regular, Regular, tripB);

        tripB.addStop(platformStopCallB);

        service.addTrip(tripA);
        service.addTrip(tripB);

        assertEquals(TramTime.of(8,16), service.getStartTime());
        assertEquals(TramTime.nextDay(0,10), service.getFinishTime());

    }

}