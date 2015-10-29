package com.tramchester.dataimport.datacleanse;

import com.tramchester.dataimport.FetchDataFromUrl;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDataCleanser extends EasyMockSupport {

    private FetchDataFromUrl fetcher;
    private TransportDataReader reader;
    private TransportDataWriter writer;
    private DataCleanser cleanser;
    private DateTimeFormatter formatter;

    @Before
    public void beforeEachTestRuns() {
        fetcher = createMock(FetchDataFromUrl.class);
        reader = createMock(TransportDataReader.class);
        writer = createMock(TransportDataWriter.class);
        cleanser = new DataCleanser(fetcher, reader, writer);
        formatter = DateTimeFormat.forPattern("HH:mm:ss");
    }

    @Test
    public void shouldCleanseRoutes() throws IOException {

        RouteData routeA = new RouteData("R2", "CODE2", "CtoD", "NOT");
        RouteData routeB = new RouteData("R1", "CODE1", "AtoB", "MET");
        Stream<RouteData> routes = Stream.of(routeA, routeB);

        EasyMock.expect(reader.getRoutes()).andReturn(routes);
        writer.writeFile("R1,MET,CODE1,AtoB,0\n", "routes");
        EasyMock.expectLastCall();

        replayAll();
        cleanser.cleanseRoutes();
        verifyAll();
    }

    @Test
    public void shouldCleanseStops() throws IOException {

        StopData stopA = new StopData("1122IdA", "codeA", "nameA", 0.11, 0.22);
        StopData stopB = new StopData("9400IdB", "codeB", "nameB", 0.33, 0.44);
        Stream<StopData> stops = Stream.of(stopA, stopB);

        EasyMock.expect(reader.getStops()).andReturn(stops);
        writer.writeFile("9400IdB,codeB,nameB,0.33,0.44\n", "stops");
        EasyMock.expectLastCall();

        replayAll();
        cleanser.cleanseStops();
        verifyAll();
    }

    @Test
    public void shouldCleanseTrips() throws IOException {

        TripData tripA = new TripData("GMBrouteIdA", "svcIdA", "tripIdA","headsignA");
        TripData tripB = new TripData("METrouteIdB", "svcIdB", "tripIdB","headsignB");
        Stream<TripData> trips = Stream.of(tripA, tripB);

        EasyMock.expect(reader.getTrips()).andReturn(trips);
        writer.writeFile("METrouteIdB,svcIdB,tripIdB,headsignB\n", "trips");
        EasyMock.expectLastCall();

        replayAll();
        Set<String> svcIds = cleanser.cleanseTrips();
        verifyAll();

        assertEquals(1, svcIds.size());
        assertTrue(svcIds.contains("svcIdB"));
    }

    @Test
    public void shouldCleanseStopTimes() throws IOException {
        DateTime now = DateTime.now();
        DateTime arrivalTime = DateTime.parse("11:10:09", formatter);
        DateTime departureTime = DateTime.parse("12:09:29", formatter);

        StopTimeData stopTimeA = new StopTimeData("tripIdA", now.minusHours(1), now.plusHours(1), "1200stopIdA",
                "stopSeqA", "pickupA", "dropA", 18);

        StopTimeData stopTimeB = new StopTimeData("tripIdB", arrivalTime, departureTime, "9400stopIdB",
                "stopSeqB", "pickupB", "dropB", 42);

        Stream<StopTimeData> stopTimes = Stream.of(stopTimeA, stopTimeB);
        EasyMock.expect(reader.getStopTimes()).andReturn(stopTimes);
        String expected = String.format("tripIdB,%s,%s,9400stopIdB,stopSeqB,pickupB,dropB\n",
                arrivalTime.toString("HH:mm:ss"), departureTime.toString("HH:mm:ss"));
        writer.writeFile(expected, "stop_times");
        EasyMock.expectLastCall();

        replayAll();
        cleanser.cleanseStoptimes();
        verifyAll();
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
        writer.writeFile("svcIDB,1,1,1,1,1,1,1,20151025,20151026\nsvcIDC,0,1,0,0,0,0,0,20151025,20151026\n",
                "calendar");
        EasyMock.expectLastCall();

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
        writer.writeFile("pubB,urlB,tzB,landB,fromB,toB,versionB\n", "feed_info");
        EasyMock.expectLastCall();

        replayAll();
        cleanser.cleanFeedInfo();
        verifyAll();
    }
}
