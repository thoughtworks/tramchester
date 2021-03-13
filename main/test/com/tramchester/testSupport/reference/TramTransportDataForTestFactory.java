package com.tramchester.testSupport.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.repository.TransportDataFactory;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TestStation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static com.tramchester.domain.places.Station.METROLINK_PREFIX;
import static com.tramchester.domain.reference.KnownTramRoute.*;
import static java.lang.String.format;

@LazySingleton
public class TramTransportDataForTestFactory implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(TramTransportDataForTestFactory.class);

    private final TramTransportDataForTest container;

    @Inject
    public TramTransportDataForTestFactory(ProvidesNow providesNow) {
        container = new TramTransportDataForTest(providesNow);
    }

    @PostConstruct
    public void start() {
        logger.info("starting");
        populateTestData(container);
        logger.info(container.toString());
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stop");
        container.dispose();
        logger.info("stopped");
    }

    public TramTransportDataForTest getTestData() {
        return container;
    }

    public TransportData getData() {
        return getTestData();
    }

    private void populateTestData(TransportDataContainer container) {
        Agency agency = TestEnv.MetAgency();

        //Route routeA = RoutesForTesting.ALTY_TO_BURY; // TODO This route not present during lockdown
        Route routeA = RoutesForTesting.createTramRoute(CornbrookintuTraffordCentre);
        Route routeB = RoutesForTesting.createTramRoute(RochdaleManchesterEDidsbury);
        Route routeC = RoutesForTesting.createTramRoute(EDidsburyManchesterRochdale);

        agency.addRoute(routeA);
        agency.addRoute(routeB);
        agency.addRoute(routeC);

        container.addAgency(agency);

        Service serviceA = new Service(TramTransportDataForTest.serviceAId, routeA);
        Service serviceB = new Service(TramTransportDataForTest.serviceBId, routeB);
        Service serviceC = new Service(TramTransportDataForTest.serviceCId, routeC);

        routeA.addService(serviceA);
        routeB.addService(serviceB);
        routeC.addService(serviceC);

        container.addRoute(routeA);
        container.addRoute(routeB);
        container.addRoute(routeC);

        LocalDate startDate = LocalDate.of(2014, 2, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);

        ServiceCalendar serviceCalendarA = new ServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        ServiceCalendar serviceCalendarB = new ServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        ServiceCalendar serviceCalendarC = new ServiceCalendar(startDate, endDate,DayOfWeek.MONDAY);

        serviceA.setCalendar(serviceCalendarA);
        serviceB.setCalendar(serviceCalendarB);
        serviceC.setCalendar(serviceCalendarC);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        Trip tripA = new Trip(TramTransportDataForTest.TRIP_A_ID, "headSign", serviceA, routeA);

        //LatLong latLong = new LatLong(latitude, longitude);
        Station first = new TestStation(TramTransportDataForTest.FIRST_STATION, "area1", "startStation",
                TestEnv.nearAltrincham, TestEnv.nearAltrinchamGrid, TransportMode.Tram);
        addAStation(container, first);
        addRouteStation(container, first, routeA);
        PlatformStopCall stopA = createStop(container, tripA, first, TramTime.of(8, 0), TramTime.of(8, 0), 1);
        tripA.addStop(stopA);

        Station second = new TestStation(TramTransportDataForTest.SECOND_STATION, "area2", "secondStation", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid, TransportMode.Tram);
        addAStation(container, second);
        addRouteStation(container, second, routeA);
        PlatformStopCall stopB = createStop(container, tripA, second, TramTime.of(8, 11), TramTime.of(8, 11), 2);
        tripA.addStop(stopB);

        Station interchangeStation = new TestStation(TramTransportDataForTest.INTERCHANGE, "area3", "cornbrookStation", TestEnv.nearShudehill,
                TestEnv.nearShudehillGrid, TransportMode.Tram);
        addAStation(container, interchangeStation);
        addRouteStation(container, interchangeStation, routeA);
        PlatformStopCall stopC = createStop(container, tripA, interchangeStation, TramTime.of(8, 20), TramTime.of(8, 20), 3);
        tripA.addStop(stopC);

        Station last = new TestStation(TramTransportDataForTest.LAST_STATION, "area4", "endStation", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid,  TransportMode.Tram);
        addAStation(container, last);
        addRouteStation(container, last, routeA);
        PlatformStopCall stopD = createStop(container, tripA, last, TramTime.of(8, 40), TramTime.of(8, 40), 4);
        tripA.addStop(stopD);

        // service A
        serviceA.addTrip(tripA);

        Station stationFour = new TestStation(TramTransportDataForTest.STATION_FOUR, "area4", "Station4", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid,  TransportMode.Tram);
        addAStation(container, stationFour);

        Station stationFive = new TestStation(TramTransportDataForTest.STATION_FIVE, "area5", "Station5", TestEnv.nearStockportBus,
                TestEnv.nearStockportBusGrid,  TransportMode.Tram);
        addAStation(container, stationFive);

        //
        Trip tripC = new Trip("tripCId", "headSignC", serviceC, routeC);
        PlatformStopCall stopG = createStop(container, tripC, interchangeStation, TramTime.of(8, 26), TramTime.of(8, 27), 1);
        addRouteStation(container, interchangeStation, routeC);
        PlatformStopCall stopH = createStop(container, tripC, stationFive, TramTime.of(8, 31), TramTime.of(8, 33), 2);
        addRouteStation(container, stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        serviceC.addTrip(tripC);

        // INTERCHANGE -> STATION_FOUR
        addRouteStation(container, stationFour, routeB);
        addRouteStation(container, interchangeStation, routeB);

        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(container,routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 20), "tripB3Id");

        container.addTrip(tripA);
        container.addTrip(tripC);

        container.addService(serviceA);
        container.addService(serviceB);
        container.addService(serviceC);

        container.updateTimesForServices();
    }

    private void addAStation(TransportDataContainer container, Station station) {
        container.addStation(station);
    }

    private static void addRouteStation(TransportDataContainer container, Station station, Route route) {
        RouteStation routeStation = new RouteStation(station, route);
        container.addRouteStation(routeStation);
        station.addRoute(route);
    }

    private static void createInterchangeToStation4Trip(TransportDataContainer container, Route route, Service service,
                                                        Station interchangeStation, Station station, LocalTime startTime, String tripId) {
        Trip trip = new Trip(tripId, "headSignTripB2", service, route);
        PlatformStopCall stop1 = createStop(container,trip, interchangeStation, TramTime.of(startTime),
                TramTime.of(startTime.plusMinutes(5)), 1);
        trip.addStop(stop1);
        PlatformStopCall stop2 = createStop(container,trip, station, TramTime.of(startTime.plusMinutes(5)),
                TramTime.of(startTime.plusMinutes(8)), 2);
        trip.addStop(stop2);
        service.addTrip(trip);
        container.addTrip(trip);
    }

    private static PlatformStopCall createStop(TransportDataContainer container, Trip trip, Station station, TramTime arrivalTime, TramTime departureTime, int sequenceNum) {
        String platformId = station.getId() + "1";
        Platform platform = new Platform(platformId, format("%s platform 1", station.getName()), station.getLatLong());
        container.addPlatform(platform);
        station.addPlatform(platform);
        StopTimeData stopTimeData = new StopTimeData(trip.getId().forDTO(), arrivalTime, departureTime, platformId,sequenceNum,
                GTFSPickupDropoffType.Regular, GTFSPickupDropoffType.Regular);
        return new PlatformStopCall(platform, station, stopTimeData);
    }

    public static class TramTransportDataForTest extends TransportDataContainer {

        private static final String serviceAId = "serviceAId";
        private static final String serviceBId = "serviceBId";
        private static final String serviceCId = "serviceCId";

        public static final String TRIP_A_ID = "tripAId";
        public static final String FIRST_STATION = METROLINK_PREFIX + "_ST_FIRST";
        public static final String SECOND_STATION = METROLINK_PREFIX + "_ST_SECOND";
        public static final String LAST_STATION = METROLINK_PREFIX + "_ST_LAST";
        public static final String INTERCHANGE = TramStations.Cornbrook.forDTO();
        private static final String STATION_FOUR = METROLINK_PREFIX + "_ST_FOUR";
        private static final String STATION_FIVE = METROLINK_PREFIX + "_ST_FIVE";

        public TramTransportDataForTest(ProvidesNow providesNow) {
            super(providesNow, "TramTransportDataForTest");
        }

        public Station getFirst() {
            return getStationById(StringIdFor.createId(FIRST_STATION));
        }

        public Station getSecond() {
            return getStationById(StringIdFor.createId(SECOND_STATION));
        }

        public Station getInterchange() {
            return getStationById(StringIdFor.createId(INTERCHANGE));
        }

        public Station getLast() {
            return getStationById(StringIdFor.createId(LAST_STATION));
        }

        public Station getFifthStation() {
            return getStationById(StringIdFor.createId(STATION_FIVE));
        }

        public Station getFourthStation() {
            return getStationById(StringIdFor.createId(STATION_FOUR));
        }

        @Override
        public Map<DataSourceID, FeedInfo> getFeedInfos() {
            FeedInfo info = new FeedInfo("publisherName", "publisherUrl", "timezone", "lang",
                    LocalDate.of(2016, 5, 25),
                    LocalDate.of(2016, 6, 30), "version");

            Map<DataSourceID, FeedInfo> result = new HashMap<>();
            result.put(new DataSourceID("TransportDataForTest"), info);
            return result;
        }

    }
}
