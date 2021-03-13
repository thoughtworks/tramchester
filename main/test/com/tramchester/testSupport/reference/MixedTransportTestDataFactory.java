package com.tramchester.testSupport.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.data.StopTimeData;
import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.NoPlatformStopCall;
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
import com.tramchester.testSupport.TestNoPlatformStation;
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

@LazySingleton
public class MixedTransportTestDataFactory implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(MixedTransportTestDataFactory.class);

    private final MixedTransportTestData container;

    @Inject
    public MixedTransportTestDataFactory(ProvidesNow providesNow) {
        container = new MixedTransportTestData(providesNow);
    }

    @PostConstruct
    public void start() {
        logger.info("start");
        populateTestData(container);
        logger.info("started");
    }

    @PreDestroy
    public void stop() {
        logger.info("stop");
        container.dispose();
        logger.info("stopped");
    }

    public TransportData getData() {
        return container;
    }

    private static final Route FERRY_ROUTE = new Route(StringIdFor.createId("FER:42:C"), "42", "Lakes",
            new Agency(DataSourceID.GBRail(), "FER", "ferryAgency"), TransportMode.Ferry);

    private void populateTestData(TransportDataContainer container) {
        Route routeA = RoutesForTesting.AIR_TO_BUXTON;
        Route ferryRoute = FERRY_ROUTE;
        Route routeC = RoutesForTesting.ALTY_TO_STOCKPORT_WBT;

        Agency agencyA = routeA.getAgency();
        Agency agencyB = routeA.getAgency();
        Agency agencyC = routeA.getAgency();

        agencyA.addRoute(routeA);
        agencyB.addRoute(ferryRoute);
        agencyC.addRoute(routeC);

        container.addAgency(agencyA);
        container.addAgency(agencyB);
        container.addAgency(agencyC);

        Service serviceA = new Service(MixedTransportTestData.serviceAId, routeA);
        Service serviceB = new Service(MixedTransportTestData.serviceBId, ferryRoute);
        Service serviceC = new Service(MixedTransportTestData.serviceCId, routeC);

        routeA.addService(serviceA);
        ferryRoute.addService(serviceB);
        routeC.addService(serviceC);

        container.addRoute(routeA);
        container.addRoute(ferryRoute);
        container.addRoute(routeC);

        LocalDate startDate = LocalDate.of(2014, 2, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);

        ServiceCalendar serviceCalendarA = new ServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        ServiceCalendar serviceCalendarB = new ServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        ServiceCalendar serviceCalendarC = new ServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);

        serviceA.setCalendar(serviceCalendarA);
        serviceB.setCalendar(serviceCalendarB);
        serviceC.setCalendar(serviceCalendarC);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        Trip tripA = new Trip(MixedTransportTestData.TRIP_A_ID, "headSign", serviceA, routeA);

        Station first = new TestNoPlatformStation(MixedTransportTestData.FIRST_STATION, "area1", "startStation",
                TestEnv.nearAltrincham, TestEnv.nearAltrinchamGrid, TransportMode.Bus);
        addAStation(container, first);
        addRouteStation(container, first, routeA);
        NoPlatformStopCall stopA = createStop(tripA, first, TramTime.of(8, 0),
                TramTime.of(8, 0), 1, TransportMode.Bus);
        tripA.addStop(stopA);

        Station second = new TestNoPlatformStation(MixedTransportTestData.SECOND_STATION, "area2", "secondStation", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid, TransportMode.Bus);
        addAStation(container, second);
        addRouteStation(container, second, routeA);
        NoPlatformStopCall stopB = createStop(tripA, second, TramTime.of(8, 11),
                TramTime.of(8, 11), 2, TransportMode.Bus);
        tripA.addStop(stopB);

        Station interchangeStation = new TestNoPlatformStation(MixedTransportTestData.INTERCHANGE, "area3", "cornbrookStation", TestEnv.nearShudehill,
                TestEnv.nearShudehillGrid, TransportMode.Bus);
        addAStation(container, interchangeStation);
        addRouteStation(container, interchangeStation, routeA);
        NoPlatformStopCall stopC = createStop(tripA, interchangeStation, TramTime.of(8, 20),
                TramTime.of(8, 20), 3, TransportMode.Bus);
        tripA.addStop(stopC);

        Station last = new TestNoPlatformStation(MixedTransportTestData.LAST_STATION, "area4", "endStation", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid,  TransportMode.Bus);
        addAStation(container, last);
        addRouteStation(container, last, routeA);
        NoPlatformStopCall stopD = createStop(tripA, last, TramTime.of(8, 40),
                TramTime.of(8, 40), 4, TransportMode.Bus);
        tripA.addStop(stopD);

        // service A
        serviceA.addTrip(tripA);

        Station stationFour = new TestNoPlatformStation(MixedTransportTestData.STATION_FOUR, "area4", "Station4", TestEnv.nearPiccGardens,
                TestEnv.nearPiccGardensGrid,  TransportMode.Bus);
        addAStation(container, stationFour);

        Station stationFive = new TestNoPlatformStation(MixedTransportTestData.STATION_FIVE, "area5", "Station5", TestEnv.nearStockportBus,
                TestEnv.nearStockportBusGrid,  TransportMode.Bus);
        addAStation(container, stationFive);

        //
        Trip tripC = new Trip("tripCId", "headSignC", serviceC, routeC);
        NoPlatformStopCall stopG = createStop(tripC, interchangeStation, TramTime.of(8, 26),
                TramTime.of(8, 27), 1, TransportMode.Bus);
        addRouteStation(container, interchangeStation, routeC);
        NoPlatformStopCall stopH = createStop(tripC, stationFive, TramTime.of(8, 31),
                TramTime.of(8, 33), 2, TransportMode.Bus);
        addRouteStation(container, stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        serviceC.addTrip(tripC);

        // INTERCHANGE -> STATION_FOUR
        addRouteStation(container, stationFour, ferryRoute);
        addRouteStation(container, interchangeStation, ferryRoute);

        createInterchangeToStation4Trip(container,ferryRoute, serviceB, interchangeStation, stationFour, LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(container,ferryRoute, serviceB, interchangeStation, stationFour, LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(container,ferryRoute, serviceB, interchangeStation, stationFour, LocalTime.of(9, 20), "tripB3Id");

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
        NoPlatformStopCall stop1 = createStop(trip, interchangeStation, TramTime.of(startTime),
                TramTime.of(startTime.plusMinutes(5)), 1, TransportMode.Bus);
        trip.addStop(stop1);
        NoPlatformStopCall stop2 = createStop(trip, station, TramTime.of(startTime.plusMinutes(5)),
                TramTime.of(startTime.plusMinutes(8)), 2, TransportMode.Bus);
        trip.addStop(stop2);
        service.addTrip(trip);
        container.addTrip(trip);
    }

    private static NoPlatformStopCall createStop(Trip trip, Station station,
                                                 TramTime arrivalTime, TramTime departureTime, int sequenceNum, TransportMode mode) {
        StopTimeData stopTimeData = new StopTimeData(trip.getId().forDTO(), arrivalTime, departureTime, station.forDTO(),
                sequenceNum, GTFSPickupDropoffType.Regular, GTFSPickupDropoffType.Regular);
        return new NoPlatformStopCall(station, stopTimeData);
    }


    public static class MixedTransportTestData extends TransportDataContainer {

        private static final String serviceAId = "serviceAId";
        private static final String serviceBId = "serviceBId";
        private static final String serviceCId = "serviceCId";

        public static final String TRIP_A_ID = "tripAId";
        public static final String PREFIX = "XXX";
        public static final String FIRST_STATION = PREFIX + "_ST_FIRST";
        public static final String SECOND_STATION = PREFIX + "_ST_SECOND";
        public static final String LAST_STATION = PREFIX + "_ST_LAST";
        public static final String INTERCHANGE = PREFIX + "_INTERCHANGE";
        public static final String STATION_FOUR = PREFIX + "_ST_FOUR";
        public static final String STATION_FIVE = PREFIX + "_ST_FIVE";

        public MixedTransportTestData(ProvidesNow providesNow) {
            super(providesNow, "MixedTransportTestData");
        }

        @Override
        public void addStation(Station station) {
            super.addStation(station);
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
