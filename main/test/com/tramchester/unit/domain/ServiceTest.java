package com.tramchester.unit.domain;

import com.tramchester.dataimport.data.StopTimeData;
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
                "1", TestEnv.nearWythenshaweHosp);
        Platform platformB = new MutablePlatform(StringIdFor.createId("platB"), "shudehill",
                "2", TestEnv.nearShudehill);

        StopTimeData stopTimeDataA = StopTimeData.forTestOnly("tripA", TramTime.of(8,15), TramTime.of(8,16),
                "shudehill", 1, Regular, Regular);
        StopTimeData stopTimeDataB = StopTimeData.forTestOnly("tripB", TramTime.of(16,25), TramTime.of(16,26),
                "shudehill", 1, Regular, Regular);

        Route route = TestEnv.getTramTestRoute();
        MutableService service = new MutableService(StringIdFor.createId("svcA"));

        final MutableTrip tripA = new MutableTrip(StringIdFor.createId("tripA"), "headSignA", service, route, Tram);
        tripA.addStop(new PlatformStopCall(tripA, platformA, of(ManAirport), stopTimeDataA));

        final MutableTrip tripB = new MutableTrip(StringIdFor.createId("tripB"), "headSignB", service, route, Tram);
        tripB.addStop(new PlatformStopCall(tripB, platformB, of(Shudehill), stopTimeDataB));

        service.addTrip(tripA);
        service.addTrip(tripB);

        assertEquals(TramTime.of(8,16), service.getStartTime());
        assertEquals(TramTime.of(16,25), service.getFinishTime());

    }

    @Test
    void shouldHaveTimeRangeCrossesMidnight() {
        Platform platformA = new MutablePlatform(StringIdFor.createId("platA"), "man airport",
                "1", TestEnv.nearWythenshaweHosp);
        Platform platformB = new MutablePlatform(StringIdFor.createId("platB"), "shudehill",
                "2", TestEnv.nearShudehill);

        StopTimeData stopTimeDataA = StopTimeData.forTestOnly("tripA",
                TramTime.of(8,15), TramTime.of(8,16),
                "shudehill", 1, Regular, Regular);
        StopTimeData stopTimeDataB = StopTimeData.forTestOnly("tripB",
                TramTime.nextDay(0,10), TramTime.nextDay(0,15),
                "shudehill", 1, Regular, Regular);

        Route route = TestEnv.getTramTestRoute();
        MutableService service = new MutableService(StringIdFor.createId("svcA"));

        final MutableTrip tripA = new MutableTrip(StringIdFor.createId("tripA"), "headSignA", service, route, Tram);
        tripA.addStop(new PlatformStopCall(tripA, platformA, of(ManAirport), stopTimeDataA));

        final MutableTrip tripB = new MutableTrip(StringIdFor.createId("tripB"), "headSignB", service, route, Tram);
        tripB.addStop(new PlatformStopCall(tripB, platformB, of(Shudehill), stopTimeDataB));

        service.addTrip(tripA);
        service.addTrip(tripB);

        assertEquals(TramTime.of(8,16), service.getStartTime());
        assertEquals(TramTime.nextDay(0,10), service.getFinishTime());

    }

}