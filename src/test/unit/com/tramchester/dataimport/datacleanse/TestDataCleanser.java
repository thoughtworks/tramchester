package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.data.*;
import com.tramchester.domain.FeedInfo;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDataCleanser extends EasyMockSupport {

    private TransportDataReader reader;
    private TransportDataWriter writer;
    private DataCleanser cleanser;
    private TransportDataWriterFactory factory;

    @Before
    public void beforeEachTestRuns() {
        reader = createMock(TransportDataReader.class);
        writer = createMock(TransportDataWriter.class);
        factory = createMock(TransportDataWriterFactory.class);
        cleanser = new DataCleanser(reader, factory);
    }

    @Test
    public void shouldCleanseRoutes() throws IOException {

        RouteData routeA = new RouteData("R2", "CODE2", "CtoD", "NOT");
        RouteData routeB = new RouteData("R1", "CODE1", "AtoB", "MET");
        Stream<RouteData> routes = Stream.of(routeA, routeB);

        EasyMock.expect(reader.getRoutes()).andReturn(routes);
        validateWriter("routes", "R1,MET,CODE1,AtoB,0");

        replayAll();
        List<String> routeCodes = cleanser.cleanseRoutes(new HashSet<>(Arrays.asList("MET")));
        verifyAll();

        assertEquals(1, routeCodes.size());
        assertEquals("R1", routeCodes.get(0));
    }

    @Test
    public void shouldCleanseStops() throws IOException {

        StopData stopA = new StopData("1122IdA", "codeA", "nameA", 0.11, 0.22);
        StopData stopB = new StopData("9400IdB", "codeB", "nameB", 0.33, 0.44);
        Stream<StopData> stops = Stream.of(stopA, stopB);

        EasyMock.expect(reader.getStops()).andReturn(stops);
        validateWriter("stops", "9400IdB,codeB,nameB,0.33,0.44");

        Set<String> stopIds = new HashSet<>();
        stopIds.add("9400IdB");

        replayAll();
        cleanser.cleanseStops(stopIds);
        verifyAll();
    }

    @Test
    public void shouldCleanseTrips() throws IOException {
        TripData tripA = new TripData("GMBrouteIdA", "svcIdA", "tripIdA","headsignA");
        TripData tripB = new TripData("METrouteIdB", "svcIdB", "tripIdB","headsignB");
        TripData tripC = new TripData("METrouteIdB", "svcIdB", "tripIdC","headsignC");
        Stream<TripData> trips = Stream.of(tripA, tripB, tripC);

        EasyMock.expect(reader.getTrips()).andReturn(trips);
        validateWriter("trips", "METrouteIdB,svcIdB,tripIdB,headsignB", "METrouteIdB,svcIdB,tripIdC,headsignC");

        replayAll();
        ServicesAndTrips servicesAndTrips = cleanser.cleanseTrips(Arrays.asList("METrouteIdB"));
        verifyAll();

        Set<String> serviceIds = servicesAndTrips.getServiceIds();
        assertEquals(1, serviceIds.size());
        assertTrue(serviceIds.contains("svcIdB"));
        Set<String> tripIds = servicesAndTrips.getTripIds();
        assertEquals(2, tripIds.size());
        assertTrue(tripIds.contains("tripIdB"));
        assertTrue(tripIds.contains("tripIdC"));
    }

    @Test
    public void shouldCleanseStopTimes() throws IOException {
        LocalTime now = LocalTime.now();
        LocalTime arrivalTime = LocalTime.parse("11:10:09");    //, formatter);
        LocalTime departureTime = LocalTime.parse("12:09:29");  //, formatter);

        StopTimeData stopTimeA = new StopTimeData("tripIdA", now.minusHours(1), now.plusHours(1), "1200stopIdA",
                "stopSeqA", "pickupA", "dropA", 18);

        StopTimeData stopTimeB = new StopTimeData("tripIdB", arrivalTime, departureTime, "9400stopIdB",
                "stopSeqB", "pickupB", "dropB", 42);

        Stream<StopTimeData> stopTimes = Stream.of(stopTimeA, stopTimeB);

        EasyMock.expect(reader.getStopTimes()).andReturn(stopTimes);
        validateWriter("stop_times", String.format("tripIdB,%s,%s,9400stopIdB,stopSeqB,pickupB,dropB",
                arrivalTime.toString(), departureTime.toString()));

        replayAll();
        Set<String> trips = new HashSet<>();
        trips.add("tripIdB");
        Set<String> stopIds = cleanser.cleanseStoptimes(trips);
        verifyAll();
        assertEquals(1, stopIds.size());
        assertTrue(stopIds.contains("9400stopIdB"));
    }

    @Test
    public void shouldcleanseCalendar() throws IOException {
        Set<String> svcIds = new HashSet<>();
        svcIds.add("svcIDA");
        svcIds.add("svcIDB");
        svcIds.add("svcIDC");

        DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(("yyyyMMdd"));
        DateTime start = DateTime.parse("20151025", dateFormatter);
        DateTime end = DateTime.parse("20151026", dateFormatter);
        CalendarData dayA = new CalendarData("svcIDA", false, false, false, false, false, false, false, start, end);
        CalendarData dayB = new CalendarData("svcIDB", true, true, true, true, true, true, true, start, end);
        CalendarData dayC = new CalendarData("svcIDC", false, true, false, false, false, false, false, start, end);
        CalendarData dayD = new CalendarData("svcIDD", true, true, true, true, true, true, true, start, end);

        Stream<CalendarData> calendar = Stream.of(dayA, dayB, dayC, dayD);
        EasyMock.expect(reader.getCalendar()).andReturn(calendar);
        validateWriter("calendar", "svcIDB,1,1,1,1,1,1,1,20151025,20151026", "svcIDC,0,1,0,0,0,0,0,20151025,20151026");

        replayAll();
        cleanser.cleanseCalendar(svcIds);
        verifyAll();
    }

    @Test
    public void shoudRemoveHeaderFromFeedInfo() throws IOException {

        FeedInfo lineA = new FeedInfo("pubA", "urlA", "tzA", "landA", "fromA", "toA", "versionA");
        FeedInfo lineB = new FeedInfo("pubB", "urlB", "tzB", "landB", "fromB", "toB", "versionB");

        Stream<FeedInfo> feedInfo = Stream.of(lineA, lineB);
        EasyMock.expect(reader.getFeedInfo()).andReturn(feedInfo);
        validateWriter("feed_info",  "pubB,urlB,tzB,landB,fromB,toB,versionB");
        EasyMock.expectLastCall();

        replayAll();
        cleanser.cleanFeedInfo();
        verifyAll();
    }

    private void validateWriter(String filename, String... lines) throws IOException {
        EasyMock.expect(factory.getWriter(filename)).andReturn(writer);
        for (String line : lines) {
            writer.writeLine(line);
            EasyMock.expectLastCall();
        }
        writer.close();
        EasyMock.expectLastCall();
    }
}
