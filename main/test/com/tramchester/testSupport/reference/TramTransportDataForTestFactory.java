package com.tramchester.testSupport.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.loader.TransportDataFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.PlatformStopCall;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
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

import static com.tramchester.domain.reference.TransportMode.Tram;
import static com.tramchester.domain.time.TramTime.of;
import static com.tramchester.testSupport.reference.KnownLocations.*;
import static com.tramchester.testSupport.reference.KnownTramRoute.*;
import static com.tramchester.testSupport.reference.TramTransportDataForTestFactory.TramTransportDataForTest.INTERCHANGE;
import static java.lang.String.format;

@LazySingleton
public class TramTransportDataForTestFactory implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(TramTransportDataForTestFactory.class);

    private final TramTransportDataForTest container;

    private final DataSourceID dataSourceID = DataSourceID.tfgm;

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
        MutableAgency agency =  new MutableAgency(DataSourceID.tfgm, MutableAgency.METL, "Metrolink");

        MutableRoute routeA = createTramRoute(CornbrookTheTraffordCentre);
        MutableRoute routeB = createTramRoute(RochdaleShawandCromptonManchesterEastDidisbury);
        MutableRoute routeC = createTramRoute(EastDidisburyManchesterShawandCromptonRochdale);
        MutableRoute routeD = createTramRoute(ManchesterAirportWythenshaweVictoria);

        agency.addRoute(routeA);
        agency.addRoute(routeB);
        agency.addRoute(routeC);
        agency.addRoute(routeD);

        container.addAgency(agency);

        MutableService serviceA = new MutableService(TramTransportDataForTest.serviceAId);
        MutableService serviceB = new MutableService(TramTransportDataForTest.serviceBId);
        MutableService serviceC = new MutableService(TramTransportDataForTest.serviceCId);
        MutableService serviceD = new MutableService(TramTransportDataForTest.serviceDId);

        LocalDate startDate = LocalDate.of(2014, 2, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);

        MutableServiceCalendar serviceCalendarA = new MutableServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        MutableServiceCalendar serviceCalendarB = new MutableServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        MutableServiceCalendar serviceCalendarC = new MutableServiceCalendar(startDate, endDate,DayOfWeek.MONDAY);
        MutableServiceCalendar serviceCalendarD = new MutableServiceCalendar(startDate, endDate,DayOfWeek.MONDAY);

        serviceA.setCalendar(serviceCalendarA);
        serviceB.setCalendar(serviceCalendarB);
        serviceC.setCalendar(serviceCalendarC);
        serviceD.setCalendar(serviceCalendarD);

        routeA.addService(serviceA);
        routeB.addService(serviceB);
        routeC.addService(serviceC);
        routeD.addService(serviceD);

        container.addRoute(routeA);
        container.addRoute(routeB);
        container.addRoute(routeC);
        container.addRoute(routeD);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        MutableTrip tripA = new MutableTrip(StringIdFor.createId(TramTransportDataForTest.TRIP_A_ID), "headSign",
                serviceA, routeA, Tram);
        serviceA.addTrip(tripA);

        MutableStation first = createStation(TramTransportDataForTest.FIRST_STATION, StringIdFor.createId("area1"), "startStation",
                nearAltrincham, dataSourceID);
        addAStation(container, first);
        addRouteStation(container, first, routeA);
        PlatformStopCall stopA = createStop(container, tripA, first, of(7, 55), of(8, 0), 1);
        tripA.addStop(stopA);

        // trip Z, firstNameDup - for composite station testing
        MutableTrip tripZ = new MutableTrip(StringIdFor.createId("tripZ"), "for dup", serviceD, routeD, Tram);
        serviceD.addTrip(tripZ);
        MutableStation firstDupName = createStation(TramTransportDataForTest.FIRST_STATION_DUP_NAME,
                StringIdFor.createId("area1"), "startStation", nearAltrincham, dataSourceID);
        addAStation(container, firstDupName);
        addRouteStation(container, firstDupName, routeD);
        PlatformStopCall stopZ = createStop(container, tripZ, firstDupName, of(12, 0), of(12, 0), 1);
        tripZ.addStop(stopZ);

        // trip Z, firstNameDup2 - for composite station testing
        MutableStation firstDup2Name = createStation(TramTransportDataForTest.FIRST_STATION_DUP2_NAME,
                StringIdFor.createId("area1"), "startStation", nearAltrincham, dataSourceID);
        addAStation(container, firstDup2Name);
        addRouteStation(container, firstDup2Name, routeD);
        PlatformStopCall stopZZ = createStop(container, tripZ, firstDup2Name, of(12, 0), of(12, 0), 2);
        tripZ.addStop(stopZZ);

        routeD.addTrip(tripZ);

        MutableStation second = createStation(TramTransportDataForTest.SECOND_STATION, StringIdFor.createId("area1"), "secondStation",
                atRoundthornTram, dataSourceID);
        addAStation(container, second);
        addRouteStation(container, second, routeA);
        PlatformStopCall stopB = createStop(container, tripA, second, of(8, 11), of(8, 11), 2);
        tripA.addStop(stopB);

        MutableStation interchangeStation = createStation(INTERCHANGE, StringIdFor.createId("area3"), "cornbrookStation",
                nearShudehill, dataSourceID);
        addAStation(container, interchangeStation);
        addRouteStation(container, interchangeStation, routeA);
        PlatformStopCall stopC = createStop(container, tripA, interchangeStation, of(8, 20),
                of(8, 20), 3);
        tripA.addStop(stopC);

        MutableStation last = createStation(TramTransportDataForTest.LAST_STATION, StringIdFor.createId("area4"),
                "endStation", nearPiccGardens, dataSourceID);
        addAStation(container, last);
        addRouteStation(container, last, routeA);
        PlatformStopCall stopD = createStop(container, tripA, last, of(8, 40), of(8, 40), 4);
        tripA.addStop(stopD);

        // service A
        routeA.addTrip(tripA);
        MutableStation stationFour = createStation(TramTransportDataForTest.STATION_FOUR, StringIdFor.createId("area4"),
                "Station4", nearKnutsfordBusStation, dataSourceID);
        addAStation(container, stationFour);

        // trip ZZ, fourthNameDup - for composite station testing
        MutableTrip tripZZ = new MutableTrip(StringIdFor.createId("tripZZ"), "for dup of 4", serviceA, routeD, Tram);
        serviceA.addTrip(tripZZ);
        MutableStation fourDupName = createStation(TramTransportDataForTest.STATION_FOUR_DUP_NAME,
                StringIdFor.createId("area4"), "Station4", nearKnutsfordBusStation, dataSourceID);
        addAStation(container, fourDupName);
        addRouteStation(container, fourDupName, routeD);
        PlatformStopCall fourDupStop = createStop(container, tripZZ, fourDupName,
                of(13, 0), of(13, 0), 1);
        tripZZ.addStop(fourDupStop);

        routeD.addTrip(tripZZ);

        MutableStation stationFive = createStation(TramTransportDataForTest.STATION_FIVE, StringIdFor.createId("area5"),
                "Station5", nearStockportBus, dataSourceID);
        addAStation(container, stationFive);

        //
        MutableTrip tripC = new MutableTrip(StringIdFor.createId("tripCId"), "headSignC", serviceC, routeC, Tram);
        serviceC.addTrip(tripC);
        PlatformStopCall stopG = createStop(container, tripC, interchangeStation, of(8, 26),
                of(8, 27), 1);
        addRouteStation(container, interchangeStation, routeC);
        PlatformStopCall stopH = createStop(container, tripC, stationFive, of(8, 31),
                of(8, 33), 2);
        addRouteStation(container, stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        routeC.addTrip(tripC);

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

        container.reportNumbers();
    }

    private MutableStation createStation(String station, IdFor<NaptanArea> areaId, String stationName, KnownLocations knownLocation,
                                         DataSourceID dataSourceID) {
        return new MutableStation(Station.createId(station), areaId, stationName, knownLocation.latLong(), knownLocation.grid(), dataSourceID);
    }

//    private MutableStation createStation(String station, IdFor<NaptanArea> areaId, String stationName, LatLong latLong, GridPosition gridPosition,
//                                         DataSourceID dataSourceID) {
//        return new MutableStation(Station.createId(station), areaId, stationName, latLong, gridPosition, dataSourceID);
//    }

    private MutableRoute createTramRoute(KnownTramRoute knownRoute) {
        return new MutableRoute(knownRoute.getFakeId(), knownRoute.shortName(), knownRoute.name(), TestEnv.MetAgency(),
                knownRoute.mode());
    }

    private void addAStation(TransportDataContainer container, MutableStation station) {
        container.addStation(station);
    }

    private static void addRouteStation(TransportDataContainer container, MutableStation station, Route route) {
        RouteStation routeStation = new RouteStation(station, route);
        container.addRouteStation(routeStation);
        station.addRoutePickUp(route);
        station.addRouteDropOff(route);
    }

    private static void createInterchangeToStation4Trip(TransportDataContainer container, MutableRoute route, MutableService service,
                                                        MutableStation interchangeStation, MutableStation station, LocalTime startTime, String tripId) {
        MutableTrip trip = new MutableTrip(StringIdFor.createId(tripId), "headSignTripB2", service, route, Tram);
        PlatformStopCall stop1 = createStop(container,trip, interchangeStation, of(startTime),
                of(startTime.plusMinutes(5)), 1);
        trip.addStop(stop1);
        PlatformStopCall stop2 = createStop(container,trip, station, of(startTime.plusMinutes(5)),
                of(startTime.plusMinutes(8)), 2);
        trip.addStop(stop2);
        route.addTrip(trip);
        service.addTrip(trip);
        container.addTrip(trip);
    }

    private static PlatformStopCall createStop(TransportDataContainer container, MutableTrip trip,
                                               MutableStation station, TramTime arrivalTime, TramTime departureTime, int sequenceNum) {
        String platformId = station.getId() + "1";
        final String platformName = format("%s platform 1", station.getName());

        MutablePlatform platform = new MutablePlatform(StringIdFor.createId(platformId),
                platformName, station.getDataSourceID(), "1",
                station.getAreaId(), station.getLatLong(), station.getGridPosition(), station.isMarkedInterchange());

        container.addPlatform(platform);
        station.addPlatform(platform);

        return new PlatformStopCall(platform, station, arrivalTime, departureTime, sequenceNum,
                GTFSPickupDropoffType.Regular, GTFSPickupDropoffType.Regular, trip);
    }

    public static class TramTransportDataForTest extends TransportDataContainer {

        private static final IdFor<Service> serviceAId = StringIdFor.createId("serviceAId");
        private static final IdFor<Service> serviceBId = StringIdFor.createId("serviceBId");
        private static final IdFor<Service> serviceCId = StringIdFor.createId("serviceCId");
        private static final IdFor<Service> serviceDId = StringIdFor.createId("serviceDId");

        private static final String METROLINK_PREFIX = "9400ZZ";

        public static final String TRIP_A_ID = "tripAId";
        public static final String FIRST_STATION = METROLINK_PREFIX + "FIRST";
        public static final String FIRST_STATION_DUP_NAME = METROLINK_PREFIX + "FIRSTDUP";
        public static final String FIRST_STATION_DUP2_NAME = METROLINK_PREFIX + "FIRSTDUP2";
        public static final String SECOND_STATION = METROLINK_PREFIX + "SECOND";
        public static final String LAST_STATION = METROLINK_PREFIX + "LAST";
        public static final String INTERCHANGE = TramStations.Cornbrook.getRawId();
        private static final String STATION_FOUR = METROLINK_PREFIX + "FOUR";
        private static final String STATION_FOUR_DUP_NAME = METROLINK_PREFIX + "FOURDUP";
        private static final String STATION_FIVE = METROLINK_PREFIX + "FIVE";

        public TramTransportDataForTest(ProvidesNow providesNow) {
            super(providesNow, "TramTransportDataForTest");
        }

        public Station getFirst() {
            return getStationById(StringIdFor.createId(FIRST_STATION));
        }

        public Station getFirstDupName() {
            return getStationById(StringIdFor.createId(FIRST_STATION_DUP_NAME));
        }

        public Station getFirstDup2Name() {
            return getStationById(StringIdFor.createId(FIRST_STATION_DUP2_NAME));
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

        public Station getFourthStationDupName() {
            return getStationById(StringIdFor.createId(STATION_FOUR_DUP_NAME));
        }

        public Route getRouteA() {
            return getRouteById(CornbrookTheTraffordCentre.getFakeId());
        }

        public Route getRouteB() {
            return getRouteById(RochdaleShawandCromptonManchesterEastDidisbury.getFakeId());
        }

        public Route getRouteC() {
            return getRouteById(EastDidisburyManchesterShawandCromptonRochdale.getFakeId());
        }

        public Route getRouteD() {
            return getRouteById(ManchesterAirportWythenshaweVictoria.getFakeId());
        }

        @Override
        public Map<DataSourceID, FeedInfo> getFeedInfos() {
            FeedInfo info = new FeedInfo("publisherName", "publisherUrl", "timezone", "lang",
                    LocalDate.of(2016, 5, 25),
                    LocalDate.of(2016, 6, 30), "version");

            Map<DataSourceID, FeedInfo> result = new HashMap<>();
            result.put(DataSourceID.unknown, info);
            return result;
        }

    }
}
