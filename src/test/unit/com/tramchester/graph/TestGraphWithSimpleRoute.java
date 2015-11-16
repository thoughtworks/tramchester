package com.tramchester.graph;

import com.tramchester.domain.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

public class TestGraphWithSimpleRoute {

    public static final String TMP_DB = "tmp.db";
    private static TransportDataForTest transportData;
    private static RouteCalculator calculator;
    private TramServiceDate queryDate;

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException {
        FileUtils.deleteDirectory(new File(TMP_DB));
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDBService = graphDatabaseFactory.newEmbeddedDatabase(TMP_DB);

        transportData = new TransportDataForTest();
        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData);
        builder.buildGraph();

        calculator = new RouteCalculator(graphDBService);
    }

    @Before
    public void beforeEachTestRuns() {
        queryDate = new TramServiceDate("20140630");
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("9400ZZMAABM", "9400ZZMAALT", 8*60, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("9400ZZMAABM", Interchanges.CORNBROOK, 8*60, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("9400ZZMAABM", Interchanges.CORNBROOK, 9*60, DaysOfWeek.Monday, queryDate);
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("9400ZZMAABM", "9400ZZMAANC", 8*60, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("9400ZZMAABM", "9400ZZMABNR", 8*60, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    private static class TransportDataForTest implements TransportData {
        String serviceAId = "serviceAId";
        String serviceBId = "serviceBId";

        private Collection<Route> routes;

        public TransportDataForTest() {
            routes = new LinkedList<>();
            Route routeA = new Route("routeAId", "routeACode", "routeA", "MET");
            Route routeB = new Route("routeBId", "routeBCode", "routeB", "MET");

            routes.add(routeA);
            routes.add(routeB);

            Service serviceA = new Service(serviceAId, routeA.getId());
            routeA.addService(serviceA);
            Service serviceB = new Service(serviceBId, routeB.getId());
            routeB.addService(serviceB);

            serviceA.setDays(true, false, false, false, false, false, false);
            serviceB.setDays(true, false, false, false, false, false, false);

            DateTime startDate = new DateTime(2014, 02, 10, 0, 0);
            DateTime endDate = new DateTime(2015, 8, 15, 0, 0);
            serviceA.setServiceDateRange(startDate, endDate);
            serviceB.setServiceDateRange(startDate, endDate);

            // trip: 1 -> 2 -> cornbrook -> 3
            Trip tripA = new Trip("trip1Id", "headSign", serviceAId);
            serviceA.addTrip(tripA);
            int startTime = 8*60; // 8am

            Station station = new Station("9400ZZMAABM2", "startStation", 180.00, 270.0);
            tripA.addStop(createStop(station, createTime(8, 0), createTime(8, 3), startTime+3));

            station = new Station("9400ZZMAALT1", "secondStation", 180.00, 270.0);
            tripA.addStop(createStop(station, createTime(8, 6), createTime(8, 7), startTime + 6));

            Station interchangeStation = new Station(Interchanges.CORNBROOK+"1", "cornbrook", 180.00, 270.00);
            tripA.addStop(createStop(interchangeStation, createTime(8, 20), createTime(8, 21), startTime + 20));

            station = new Station("9400ZZMAANC2", "endStation", 180.00, 270.00);
            tripA.addStop(createStop(station, createTime(8, 40), createTime(00, 41),
                    RouteCalculator.MAX_WAIT_TIME_MINS + startTime));

            // cornbrook -> 4
            Trip tripB = new Trip("trip2Id", "headSign", serviceBId);
            serviceB.addTrip(tripB);
            tripB.addStop(createStop(interchangeStation, createTime(8,26), createTime(8,27),
                    startTime+RouteCalculator.MAX_WAIT_TIME_MINS+2));

            station = new Station("9400ZZMABNR2", "stat4Station", 170.00, 160.00);
            tripB.addStop(createStop(station, createTime(8,35), createTime(8,36),
                    startTime + RouteCalculator.MAX_WAIT_TIME_MINS+10));

        }

        private LocalTime createTime(int hourOfDay, int minuteOfHour) {
            return LocalTime.of(hourOfDay,minuteOfHour,00);
        }

        private Stop createStop(Station startStation, LocalTime arrivalTime, LocalTime departureTime,
                                int minutesFromMidnight) {
           return new Stop(arrivalTime, departureTime, startStation, minutesFromMidnight);

        }

        @Override
        public Collection<Route> getRoutes() {
            return routes;
        }

        @Override
        public Route getRoute(String routeId) {
            return null;
        }

        @Override
        public List<Station> getStations() {
            return null;
        }

        @Override
        public FeedInfo getFeedInfo() {
            return new FeedInfo("publisherName", "publisherUrl", "timezone", "lang", "validFrom",
                    "validUntil", "version");
        }
    }
}
