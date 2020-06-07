package com.tramchester.unit.dataimport.datacleanse;

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
import com.tramchester.domain.time.ProvidesLocalNow;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

class TestDataCleanser extends EasyMockSupport {

    private TransportDataReader reader;
    private TransportDataWriter writer;
    private DataCleanser cleanser;
    private TransportDataWriterFactory writerFactory;
    private TransportDataReaderFactory readerFactory;
    private ProvidesNow providesNow;

    @BeforeEach
    void beforeEachTestRuns() {
        TramchesterConfig config = TestEnv.GET();
        providesNow = new ProvidesLocalNow();

        reader = createMock(TransportDataReader.class);
        writer = createMock(TransportDataWriter.class);
        writerFactory = createMock(TransportDataWriterFactory.class);
        readerFactory = createMock(TransportDataReaderFactory.class);
        cleanser = new DataCleanser(readerFactory, writerFactory, providesNow, config);
    }

    @Test
    void shouldCleanseRoutesTram() throws IOException {

        RouteData routeB = new RouteData("R1", "MET", "CODE1", "AtoB name with issue (ignore me", "0");
        Stream<RouteData> routes = Stream.of(routeB);

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        RouteDataMapper routeDataMapper = new RouteDataMapper(Collections.emptySet(), true);
        EasyMock.expect(reader.getRoutes(routeDataMapper)).andReturn(routes);
        validateWriter("routes", "\"R1\",\"MET\",\"CODE1\",\"AtoB name with issue\",\"0\"");

        replayAll();
        Set<String> routeCodes = cleanser.cleanseRoutes(routeDataMapper);
        verifyAll();

        Assertions.assertEquals(1, routeCodes.size());
        Assertions.assertTrue(routeCodes.contains("R1"));
    }

    @Test
    void shouldCleanseRoutesBus() throws IOException {

        RouteData routeB = new RouteData("R2", "GMS", "X58", "Altrincham - Strockport", "3");
        Stream<RouteData> routes = Stream.of(routeB);

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        RouteDataMapper routeDataMapper = new RouteDataMapper(Collections.emptySet(), true);
        EasyMock.expect(reader.getRoutes(routeDataMapper)).andReturn(routes);
        validateWriter("routes", "\"R2\",\"GMS\",\"X58\",\"Altrincham - Strockport\",\"3\"");

        replayAll();
        Set<String> routeCodes = cleanser.cleanseRoutes(routeDataMapper);
        verifyAll();

        Assertions.assertEquals(1, routeCodes.size());
        Assertions.assertTrue(routeCodes.contains("R2"));
    }

    @Test
    void shouldCleanseRoutesWildcard() throws IOException {
        RouteData routeA = new RouteData("R2", "ANY", "CODE2", "CtoD", "0");
        RouteData routeB = new RouteData("R1", "XYX", "CODE1", "AtoB", "3");
        Stream<RouteData> routes = Stream.of(routeA, routeB);
        HashSet<String> agencyCodes = new HashSet<>(Collections.singletonList("*"));

        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        RouteDataMapper routeDataMapper = new RouteDataMapper(agencyCodes, true);
        EasyMock.expect(reader.getRoutes(routeDataMapper)).andReturn(routes);
        validateWriter("routes", "\"R1\",\"XYX\",\"CODE1\",\"AtoB\",\"3\"", "\"R2\",\"ANY\",\"CODE2\",\"CtoD\",\"0\"");

        replayAll();
        Set<String> routeCodes = cleanser.cleanseRoutes(routeDataMapper);
        verifyAll();

        Assertions.assertEquals(2, routeCodes.size());
    }

    @Test
    void shouldCleanseStopsMet() throws IOException {

        StopData stopData = new StopData("9400IdB", "codeB", "areaB", "nameB", 0.33, 0.44, true);

        StopDataMapper stopDataMapper = new StopDataMapper(Collections.emptySet());
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);
        EasyMock.expect(reader.getStops(stopDataMapper)).andReturn(Stream.of(stopData));

        validateWriter("stops", "9400IdB,codeB,\"areaB,nameB (Manchester Metrolink)\",0.33,0.44");

        replayAll();
        Assertions.assertAll(() -> cleanser.cleanseStops(stopDataMapper));
        verifyAll();
    }

    @Test
    void shouldCleanseStopMultiPart() throws IOException {
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
        Assertions.assertAll(() -> cleanser.cleanseStops(stopDataMapper));
        verifyAll();
    }

    @Test
    void shouldCleanseTrips() throws IOException {
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
        Assertions.assertEquals(1, serviceIds.size());
        Assertions.assertTrue(serviceIds.contains("svcIdB"));
        Set<String> tripIds = servicesAndTrips.getTripIds();
        Assertions.assertEquals(2, tripIds.size());
        Assertions.assertTrue(tripIds.contains("tripIdB"));
        Assertions.assertTrue(tripIds.contains("tripIdC"));
    }

    @Test
    void shouldCleanseStopTimes() throws IOException {
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
        Assertions.assertEquals(1, stopIds.size());
        Assertions.assertTrue(stopIds.contains("9400stopIdB"));
    }

    @Test
    void shouldcleanseCalendar() throws IOException {

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
        Assertions.assertAll(() -> cleanser.cleanseCalendar(calendarDataMapper));
        verifyAll();
    }

    @Test
    void shouldcleanseCalendarDates() throws IOException {

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse("20151025", dateFormatter);
        CalendarDateData dayA = new CalendarDateData("svcIDA",date, 1);
        CalendarDateData dayB = new CalendarDateData("svcIDB",date, 2);
        CalendarDateData dayC = new CalendarDateData("svcIDC",date, 3);

        Stream<CalendarDateData> dataStream = Stream.of(dayA, dayB, dayC);
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        CalendarDatesDataMapper calendarDataMapper = new CalendarDatesDataMapper(Collections.emptySet());
        EasyMock.expect(reader.getCalendarDates(calendarDataMapper)).andReturn(dataStream);
        validateWriter("calendar_dates", "svcIDA,20151025,1",
                "svcIDB,20151025,2",
                "svcIDC,20151025,3");

        replayAll();
        Assertions.assertAll(() -> cleanser.cleanseCalendarDates(calendarDataMapper));
        verifyAll();
    }

    @Test
    void shouldLeaveFeedInfoLinesUntouched() throws IOException {

        FeedInfo lineA = new FeedInfo("pubA", "urlA", "tzA", "landA", LocalDate.of(2016,11,29),
                LocalDate.of(2016,11,30), "versionA");

        Stream<FeedInfo> feedInfoStream = Stream.of(lineA);
        EasyMock.expect(readerFactory.getForCleanser()).andReturn(reader);

        FeedInfoDataMapper feedInfoDataMapper = new FeedInfoDataMapper(providesNow);
        EasyMock.expect(reader.getFeedInfo(feedInfoDataMapper)).andReturn(feedInfoStream);
        validateWriter("feed_info",  "pubA,urlA,tzA,landA,20161129,20161130,versionA");

        replayAll();
        Assertions.assertAll(() -> cleanser.cleanFeedInfo(feedInfoDataMapper));
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
