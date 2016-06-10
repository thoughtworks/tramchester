package com.tramchester.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.UnknownStationException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class GraphWithSimpleRouteTest {

    public static final String TMP_DB = "tmp.db";
    public static final String FIRST_STATION = "9400ZZ_FIRST";
    public static final String SECOND_STATION = "9400ZZ_SECOND";
    public static final String LAST_STATION = "9400ZZ_LAST";
    public static final String INTERCHANGE = Interchanges.CORNBROOK;
    public static final String STATION_FOUR = "9400ZZ_FOUR";
    private static TransportDataForTest transportData;
    private static RouteCalculator calculator;
    private TramServiceDate queryDate;
    private int queryTime;

    @BeforeClass
    public static void onceBeforeAllTestRuns() throws IOException {
        File dbFile = new File(TMP_DB);
        FileUtils.deleteDirectory(dbFile);
        GraphDatabaseFactory graphDatabaseFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDBService = graphDatabaseFactory.newEmbeddedDatabase(dbFile);

        RelationshipFactory relationshipFactory = new RelationshipFactory();

        transportData = new TransportDataForTest();
        SpatialDatabaseService spatialDatabaseService = new SpatialDatabaseService(graphDBService);
        TransportGraphBuilder builder = new TransportGraphBuilder(graphDBService, transportData, relationshipFactory, spatialDatabaseService);
        builder.buildGraph();

        NodeFactory nodeFactory = new NodeFactory();
        RouteCodeToClassMapper routeIdToClass = new RouteCodeToClassMapper();
        calculator = new RouteCalculator(graphDBService, nodeFactory, relationshipFactory,
                spatialDatabaseService, new PathToStagesMapper(nodeFactory,routeIdToClass, transportData));
    }

    @Before
    public void beforeEachTestRuns() {
        queryDate = new TramServiceDate("20140630");
        queryTime = (8 * 60)-3;
    }

    @Test
    public void shouldTestSimpleJourneyIsPossible() throws UnknownStationException {
        Set<RawJourney> journeys = calculator.calculateRoute(FIRST_STATION, SECOND_STATION, queryTime, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsPossibleToInterchange() throws UnknownStationException {
        Set<RawJourney> journeys = calculator.calculateRoute(FIRST_STATION, INTERCHANGE, queryTime, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestSimpleJourneyIsNotPossible() throws UnknownStationException {
        Set<RawJourney> journeys = calculator.calculateRoute(FIRST_STATION, INTERCHANGE, 9*60, DaysOfWeek.Monday, queryDate);
        assertEquals(0, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitIsPossible() throws UnknownStationException {
        Set<RawJourney> journeys = calculator.calculateRoute(FIRST_STATION, LAST_STATION, queryTime, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    @Test
    public void shouldTestJourneyEndOverWaitLimitViaInterchangeIsPossible() throws UnknownStationException {
        Set<RawJourney> journeys = calculator.calculateRoute(FIRST_STATION, STATION_FOUR, queryTime, DaysOfWeek.Monday, queryDate);
        assertEquals(1, journeys.size());
    }

    private static class TransportDataForTest implements TransportData, StationRepository {
        String serviceAId = "serviceAId";
        String serviceBId = "serviceBId";

        Map<String, Station> stationMap = new HashMap<>();

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

            LocalDate startDate = new LocalDate(2014, 02, 10);
            LocalDate endDate = new LocalDate(2020, 8, 15);
            serviceA.setServiceDateRange(startDate, endDate);
            serviceB.setServiceDateRange(startDate, endDate);

            // 8*60=480

            // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
            Trip tripA = new Trip("trip1Id", "headSign", serviceAId);

            double latitude = 180.00;
            double longitude = 270.0;
            LatLong latLong = new LatLong(latitude, longitude);
            Station first = new Station(FIRST_STATION, "area", "startStation", latLong, true);
            addStation(first);
            tripA.addStop(createStop(first, createTime(8, 0), createTime(8, 0)));

            Station second = new Station(SECOND_STATION, "area", "secondStation", latLong, true);
            tripA.addStop(createStop(second, createTime(8, 11), createTime(8, 11)));
            addStation(second);

            Station interchangeStation = new Station(INTERCHANGE, "area", "cornbrook", latLong, true);
            tripA.addStop(createStop(interchangeStation, createTime(8, 20), createTime(8, 20)));
            addStation(interchangeStation);

            Station last = new Station(LAST_STATION, "area", "endStation", latLong, true);
            addStation(last);
            tripA.addStop(createStop(last, createTime(8, 40), createTime(8, 40)));
            // service
            serviceA.addTrip(tripA);

            // tripB: INTERCHANGE -> STATION_FOUR
            Trip tripB = new Trip("trip2Id", "headSign", serviceBId);
            tripB.addStop(createStop(interchangeStation, createTime(8,26), createTime(8,26)));

            Station four = new Station(STATION_FOUR, "area", "stat4Station", new LatLong(170.00, 160.00), true);
            addStation(four);
            tripB.addStop(createStop(four, createTime(8,36), createTime(8,36)));
            // service
            serviceB.addTrip(tripB);
        }

        private void addStation(Station first) {
            stationMap.put(first.getId(), first);
        }

        private LocalTime createTime(int hourOfDay, int minuteOfHour) {
            return LocalTime.of(hourOfDay,minuteOfHour,00);
        }

        private Stop createStop(Location startStation, LocalTime arrivalTime, LocalTime departureTime) {
           return new Stop(startStation, arrivalTime, departureTime);
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

        @Override
        public Station getStation(String stationId) {
            return stationMap.get(stationId);
        }
    }
}
