package com.tramchester.graph;

import com.tramchester.domain.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import static junit.framework.Assert.assertEquals;

public class TestGraphWithSimpleRoute {

    public static final String TMP_DB = "tmp.db";
    private static TransportDataForTest transportData;
    private static RouteCalculator calculator;

    @BeforeClass
    public static void beforeEachTestRuns() throws IOException {
        FileUtils.deleteDirectory(new File(TMP_DB));
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDBService = graphDatabaseFactory.newEmbeddedDatabase(TMP_DB);

        transportData = new TransportDataForTest();
        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData);
        builder.buildGraph();

        calculator = new RouteCalculator(graphDBService);
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("stat1Id", "stat2Id", 5, DaysOfWeek.Monday);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("stat1Id", TransportGraphBuilder.CORNBROOK, 5, DaysOfWeek.Monday);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("stat1Id", TransportGraphBuilder.CORNBROOK, 16, DaysOfWeek.Monday);
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("stat1Id", "stat3Id", 5, DaysOfWeek.Monday);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() throws UnknownStationException {
        Set<Journey> journeys = calculator.calculateRoute("stat1Id", "stat4Id", 5, DaysOfWeek.Monday);
        assertEquals(1, journeys.size());
    }

    private static class TransportDataForTest implements TransportData {
        String serviceAId = "serviceAId";
        String serviceBId = "serviceBId";

        private Collection<Route> routes;

        public TransportDataForTest() {
            routes = new LinkedList<>();
            Route routeA = new Route("routeAId", "routeACode", "routeA");
            Route routeB = new Route("routeBId", "routeBCode", "routeB");

            routes.add(routeA);
            routes.add(routeB);

            Service serviceA = new Service(serviceAId, routeA.getId());
            routeA.addService(serviceA);
            Service serviceB = new Service(serviceBId, routeB.getId());
            routeB.addService(serviceB);

            serviceA.setDays(true, false, false, false, false, false, false);
            serviceB.setDays(true, false, false, false, false, false, false);

            // trip: 1 -> 2 -> cornbrook -> 3
            Trip tripA = new Trip("trip1Id", "headSign", serviceAId);
            serviceA.addTrip(tripA);
            int startTime = 7;

            Station station = new Station("stat1Id", "stat1Code", "startStation", 180.00, 270.0);
            tripA.addStop(createStop(station, createTime(00, 7), createTime(00, 8), startTime));

            station = new Station("stat2Id", "stat2Code", "secondStation", 180.00, 270.0);
            tripA.addStop(createStop(station, createTime(00, 10), createTime(00, 11), startTime + 3));

            Station interchangeStation = new Station(TransportGraphBuilder.CORNBROOK, "stat2Code", "cornbrook", 180.00, 270.00);
            tripA.addStop(createStop(interchangeStation, createTime(00, 14), createTime(00, 15), startTime + 8));

            station = new Station("stat3Id", "stat3Code", "endStation", 180.00, 270.00);
            tripA.addStop(createStop(station, createTime(00, 14), createTime(00, 15),
                    RouteCalculator.MAX_WAIT_TIME_MINS + startTime));

            // cornbrook -> 4
            Trip tripB = new Trip("trip2Id", "headSign", serviceBId);
            serviceB.addTrip(tripB);
            tripB.addStop(createStop(interchangeStation, createTime(00,26), createTime(00,27),
                    startTime+RouteCalculator.MAX_WAIT_TIME_MINS+2));

            station = new Station("stat4Id", "stat4Code", "stat4Station", 170.00, 160.00);
            tripB.addStop(createStop(station, createTime(00,35), createTime(00,36),
                    startTime + RouteCalculator.MAX_WAIT_TIME_MINS+10));

        }

        private DateTime createTime(int hourOfDay, int minuteOfHour) {
            return new DateTime().withTime(hourOfDay, minuteOfHour, 00, 00);
        }

        private Stop createStop(Station startStation, DateTime arrivalTime, DateTime departureTime,
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
    }
}
