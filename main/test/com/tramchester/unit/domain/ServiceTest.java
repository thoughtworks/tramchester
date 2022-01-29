package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.time.DateRange;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.testSupport.reference.KnownLocations.nearShudehill;
import static com.tramchester.testSupport.reference.KnownLocations.nearWythenshaweHosp;
import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceTest {

    @Test
    void shouldNoticeNoDatesSet() {
        MutableService service = new MutableService(StringIdFor.createId("svcXXX"));
        assertFalse(service.hasCalendar());

        LocalDate startDate = LocalDate.of(2014, 10, 5);
        LocalDate endDate = LocalDate.of(2014, 12, 25);

        MutableServiceCalendar serviceCalendar = new MutableServiceCalendar(new DateRange(startDate, endDate), TestEnv.allDays());

        service.setCalendar(serviceCalendar);

        assertTrue(service.hasCalendar());

    }

    @Test
    void shouldHaveTimeRange() {
        Platform platformA = new MutablePlatform(StringIdFor.createId("platA"), "man airport",
                "1", nearWythenshaweHosp.latLong());
        Platform platformB = new MutablePlatform(StringIdFor.createId("platB"), "shudehill",
                "2", nearShudehill.latLong());

        Route route = TestEnv.getTramTestRoute();
        MutableService service = new MutableService(StringIdFor.createId("svcA"));

        final MutableTrip tripA = new MutableTrip(StringIdFor.createId("tripA"), "headSignA", service, route, Tram);

        final PlatformStopCall platformStopCallA = new PlatformStopCall(platformA, ManAirport.fake(),
                TramTime.of(8,15), TramTime.of(8,16), 1, Regular, Regular, tripA);
        tripA.addStop(platformStopCallA);

        final MutableTrip tripB = new MutableTrip(StringIdFor.createId("tripB"), "headSignB", service, route, Tram);

        final PlatformStopCall platformStopCallB = new PlatformStopCall(platformB, Shudehill.fake(),
                TramTime.of(16,25), TramTime.of(16,26), 1, Regular, Regular, tripB);
        tripB.addStop(platformStopCallB);

        service.addTrip(tripA);
        service.addTrip(tripB);

        assertEquals(TramTime.of(8,16), service.getStartTime());
        assertEquals(TramTime.of(16,25), service.getFinishTime());

    }

    @Test
    void shouldHaveTimeRangeCrossesMidnight() {
        Platform platformA = new MutablePlatform(StringIdFor.createId("platA"), "man airport",
                "1", nearWythenshaweHosp.latLong());
        Platform platformB = new MutablePlatform(StringIdFor.createId("platB"), "shudehill",
                "2", nearShudehill.latLong());

        Route route = TestEnv.getTramTestRoute();
        MutableService service = new MutableService(StringIdFor.createId("svcA"));

        final MutableTrip tripA = new MutableTrip(StringIdFor.createId("tripA"), "headSignA", service, route, Tram);

        final PlatformStopCall platformStopCallA = new PlatformStopCall(platformA, ManAirport.fake(),
                TramTime.of(8,15), TramTime.of(8,16), 1, Regular, Regular, tripA);

        tripA.addStop(platformStopCallA);

        final MutableTrip tripB = new MutableTrip(StringIdFor.createId("tripB"), "headSignB", service, route, Tram);

        final PlatformStopCall platformStopCallB = new PlatformStopCall(platformB, Shudehill.fake(),
                TramTime.nextDay(0,10), TramTime.nextDay(0,15), 1, Regular, Regular, tripB);

        tripB.addStop(platformStopCallB);

        service.addTrip(tripA);
        service.addTrip(tripB);

        assertEquals(TramTime.of(8,16), service.getStartTime());
        assertEquals(TramTime.nextDay(0,10), service.getFinishTime());

    }

}