package com.tramchester.unit.dataimport.datacleanse;

import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.TransportDataReader;
import com.tramchester.dataimport.TransportDataReaderFactory;
import com.tramchester.dataimport.data.*;
import com.tramchester.dataimport.datacleanse.DataCleanser;
import com.tramchester.dataimport.datacleanse.ServicesAndTrips;
import com.tramchester.dataimport.datacleanse.TransportDataWriter;
import com.tramchester.dataimport.datacleanse.TransportDataWriterFactory;
import com.tramchester.dataimport.parsers.*;
import com.tramchester.domain.FeedInfo;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDataCleanser extends EasyMockSupport {

    private TransportDataReader reader;
    private TransportDataWriter writer;
    private DataCleanser cleanser;
    private TransportDataWriterFactory writerFactory;
    private TramchesterConfig config;
    private TransportDataReaderFactory readerFactory;
    private ProvidesNow providesNow;

    @Before
    public void beforeEachTestRuns() {
        config = TestEnv.GET();
        providesNow = new ProvidesLocalNow();

        reader = createMock(TransportDataReader.class);
        writer = createMock(TransportDataWriter.class);
        writerFactory = createMock(TransportDataWriterFactory.class);
        readerFactory = createMock(TransportDataReaderFactory.class);
        cleanser = new DataCleanser(readerFactory, writerFactory, providesNow, config);
    }

    @Test
    public void shouldCleanseRoutesTram() throws IOException {

        RouteData routeB = new RouteData("R1", "MET", "CODE1", "AtoB name with issue (ignore me", "0");
        Stream<RouteData> routes = Stream.of(routeB);

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        RouteDataMapper routeDataMapper = new RouteDataMapper(Collections.emptySet());
        EasyMock.expect(reader.getRoutes(routeDataMapper)).andReturn(routes);
        validateWriter("routes", "\"R1\",\"MET\",\"CODE1\",\"AtoB name with issue\",\"0\"");

        replayAll();
        Set<String> routeCodes = cleanser.cleanseRoutes(routeDataMapper);
        verifyAll();

        assertEquals(1, routeCodes.size());
        assertTrue(routeCodes.contains("R1"));
    }

    @Test
    public void shouldCleanseRoutesBus() throws IOException {

        RouteData routeB = new RouteData("R2", "GMS", "X58", "Altrincham - Strockport", "3");
        Stream<RouteData> routes = Stream.of(routeB);

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        RouteDataMapper routeDataMapper = new RouteDataMapper(Collections.emptySet());
        EasyMock.expect(reader.getRoutes(routeDataMapper)).andReturn(routes);
        validateWriter("routes", "\"R2\",\"GMS\",\"X58\",\"Altrincham - Strockport\",\"3\"");

        replayAll();
        Set<String> routeCodes = cleanser.cleanseRoutes(routeDataMapper);
        verifyAll();

        assertEquals(1, routeCodes.size());
        assertTrue(routeCodes.contains("R2"));
    }

    @Test
    public void shouldCleanseRoutesWildcard() throws IOException {
        RouteData routeA = new RouteData("R2", "ANY", "CODE2", "CtoD", "0");
        RouteData routeB = new RouteData("R1", "XYX", "CODE1", "AtoB", "3");
        Stream<RouteData> routes = Stream.of(routeA, routeB);
        HashSet<String> agencyCodes = new HashSet<>(Arrays.asList("*"));

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        RouteDataMapper routeDataMapper = new RouteDataMapper(agencyCodes);
        EasyMock.expect(reader.getRoutes(routeDataMapper)).andReturn(routes);
        validateWriter("routes", "\"R1\",\"XYX\",\"CODE1\",\"AtoB\",\"3\"", "\"R2\",\"ANY\",\"CODE2\",\"CtoD\",\"0\"");

        replayAll();
        Set<String> routeCodes = cleanser.cleanseRoutes(routeDataMapper);
        verifyAll();

        assertEquals(2, routeCodes.size());
    }

    @Test
    public void shouldCleanseStopsMet() throws IOException {

        StopData stopData = new StopData("9400IdB", "codeB", "areaB", "nameB", 0.33, 0.44, true);

        StopDataMapper stopDataMapper = new StopDataMapper(Collections.emptySet());
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);
        EasyMock.expect(reader.getStops(stopDataMapper)).andReturn(Stream.of(stopData));

        validateWriter("stops", "9400IdB,codeB,\"areaB,nameB (Manchester Metrolink)\",0.33,0.44");

        replayAll();
        cleanser.cleanseStops(stopDataMapper);
        verifyAll();
    }

    @Test
    public void shouldCleanseStopMultiPart() throws IOException {
        StopData stopA = new StopData("1800EB05551", "mantdwgj", "Rusholme", "Anson Road/St. Anselm Hall (Stop B)",
                53.45412,-2.21209, false);
        Stream<StopData> stops = Stream.of(stopA);

        Set<String> stopIdsToInclude = new HashSet<>();
        stopIdsToInclude.add("1800EB05551");

        StopDataMapper stopDataMapper = new StopDataMapper(stopIdsToInclude);

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);
        EasyMock.expect(reader.getStops(stopDataMapper)).andReturn(stops);
        validateWriter("stops",
                "1800EB05551,mantdwgj,\"Rusholme,Anson Road/St. Anselm Hall (Stop B)\",53.45412,-2.21209");

        replayAll();
        cleanser.cleanseStops(stopDataMapper);
        verifyAll();
    }

    @Test
    public void shouldCleanseTrips() throws IOException {
        TripData tripB = new TripData("METrouteIdB", "svcIdB", "tripIdB","headsignB");
        TripData tripC = new TripData("METrouteIdB", "svcIdB", "tripIdC","headsignC");
        Stream<TripData> trips = Stream.of(tripB, tripC);

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        TripDataMapper tripDataMapper = new TripDataMapper(Collections.emptySet());
        EasyMock.expect(reader.getTrips(tripDataMapper)).andReturn(trips);
        validateWriter("trips", "METrouteIdB,svcIdB,tripIdB,headsignB", "METrouteIdB,svcIdB,tripIdC,headsignC");

        replayAll();
        ServicesAndTrips servicesAndTrips = cleanser.cleanseTrips(tripDataMapper);
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
        LocalTime arrivalTime = LocalTime.parse("11:10:00");
        LocalTime departureTime = LocalTime.parse("12:09:00");

        StopTimeData stopTimeB = new StopTimeData("tripIdB", TramTime.of(arrivalTime), TramTime.of(departureTime),
                "9400stopIdB", "stopSeqB", "pickupB", "dropB");

        Stream<StopTimeData> stopTimes = Stream.of(stopTimeB);
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        StopTimeDataMapper stopTimeDataMapper = new StopTimeDataMapper(Collections.emptySet());
        EasyMock.expect(reader.getStopTimes(stopTimeDataMapper)).andReturn(stopTimes);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
        validateWriter("stop_times", String.format("tripIdB,%s,%s,9400stopIdB,stopSeqB,pickupB,dropB",
                arrivalTime.format(formatter), departureTime.format(formatter)));

        replayAll();

        Set<String> stopIds = cleanser.cleanseStoptimes(stopTimeDataMapper);
        verifyAll();
        assertEquals(1, stopIds.size());
        assertTrue(stopIds.contains("9400stopIdB"));
    }

    @Test
    public void shouldcleanseCalendar() throws IOException {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate start = LocalDate.parse("20151025", dateFormatter);
        LocalDate end = LocalDate.parse("20151026", dateFormatter);
        CalendarData dayA = new CalendarData("svcIDA", false, false, false, false, false, false, false, start, end);
        CalendarData dayB = new CalendarData("svcIDB", true, true, true, true, true, true, true, start, end);
        CalendarData dayC = new CalendarData("svcIDC", false, true, false, false, false, false, false, start, end);

        Stream<CalendarData> calendar = Stream.of(dayA, dayB, dayC);
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        CalendarDataMapper calendarDataMapper = new CalendarDataMapper(Collections.emptySet());
        EasyMock.expect(reader.getCalendar(calendarDataMapper)).andReturn(calendar);
        validateWriter("calendar", "svcIDA,0,0,0,0,0,0,0,20151025,20151026",
                "svcIDB,1,1,1,1,1,1,1,20151025,20151026",
                "svcIDC,0,1,0,0,0,0,0,20151025,20151026");

        replayAll();
        cleanser.cleanseCalendar(calendarDataMapper);
        verifyAll();
    }

    @Test
    public void shouldLeaveFeedInfoLinesUntouched() throws IOException {

        FeedInfo lineA = new FeedInfo("pubA", "urlA", "tzA", "landA", LocalDate.of(2016,11,29),
                LocalDate.of(2016,11,30), "versionA");

        Stream<FeedInfo> feedInfoStream = Stream.of(lineA);
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        FeedInfoDataMapper feedInfoDataMapper = new FeedInfoDataMapper(providesNow);
        EasyMock.expect(reader.getFeedInfo(feedInfoDataMapper)).andReturn(feedInfoStream);
        validateWriter("feed_info",  "pubA,urlA,tzA,landA,20161129,20161130,versionA");

        replayAll();
        cleanser.cleanFeedInfo(feedInfoDataMapper);
        verifyAll();
    }

    private void validateWriter(String filename, String... lines) throws IOException {
        EasyMock.expect(writerFactory.getWriter(filename)).andReturn(writer);
        for (String line : lines) {
            writer.writeLine(line);
            EasyMock.expectLastCall();
        }
        writer.close();
        EasyMock.expectLastCall();
    }
}
