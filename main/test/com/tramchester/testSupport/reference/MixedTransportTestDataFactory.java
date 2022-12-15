package com.tramchester.testSupport.reference;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.dataimport.loader.TransportDataFactory;
import com.tramchester.domain.*;
import com.tramchester.domain.dates.MutableServiceCalendar;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.NoPlatformStopCall;
import com.tramchester.domain.places.MutableStation;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.domain.time.TramTime;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataContainer;
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

import static com.tramchester.domain.reference.GTFSPickupDropoffType.Regular;
import static com.tramchester.domain.reference.TransportMode.Bus;
import static com.tramchester.testSupport.reference.KnownLocations.*;

@LazySingleton
public class MixedTransportTestDataFactory implements TransportDataFactory {
    private static final Logger logger = LoggerFactory.getLogger(MixedTransportTestDataFactory.class);

    private final MixedTransportTestData container;
    private final DataSourceID dataSourceID = DataSourceID.tfgm;

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

    private void populateTestData(TransportDataContainer container) {

        MutableAgency ferryAgency = new MutableAgency(DataSourceID.rail, StringIdFor.createId("FER"), "ferryAgency");
        MutableRoute ferryRoute = new MutableRoute(StringIdFor.createId("FER:42:C"), "42", "Lakes",
                ferryAgency, TransportMode.Ferry);

        final MutableAgency highPeakBuses = new MutableAgency(DataSourceID.tfgm, StringIdFor.createId("HGP"),
                "High Peak Buses");

        final MutableAgency warringtonsOwnBuses = new MutableAgency(DataSourceID.tfgm, StringIdFor.createId("WBTR"),
                "Warringtons Own Buses");

        MutableRoute routeA = new MutableRoute(StringIdFor.createId("HGP:199:I:"), "199",
                "Manchester Airport - Stockport - Buxton Skyline", highPeakBuses, Bus);

        MutableRoute routeC = new MutableRoute(StringIdFor.createId("WBTR05A:I:"), "5A",
                "Alty to Stockport", warringtonsOwnBuses, Bus);

        highPeakBuses.addRoute(routeA);
        ferryAgency.addRoute(ferryRoute);
        warringtonsOwnBuses.addRoute(routeC);

        container.addAgency(highPeakBuses);
        container.addAgency(ferryAgency);
        container.addAgency(warringtonsOwnBuses);

        MutableService serviceA = new MutableService(MixedTransportTestData.serviceAId);
        MutableService serviceB = new MutableService(MixedTransportTestData.serviceBId);
        MutableService serviceC = new MutableService(MixedTransportTestData.serviceCId);

        TramDate startDate = TramDate.of(2014, 2, 10);
        TramDate endDate = TramDate.of(2020, 8, 15);

        MutableServiceCalendar serviceCalendarA = new MutableServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        serviceA.setCalendar(serviceCalendarA);

        MutableServiceCalendar serviceCalendarB = new MutableServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        serviceB.setCalendar(serviceCalendarB);

        MutableServiceCalendar serviceCalendarC = new MutableServiceCalendar(startDate, endDate, DayOfWeek.MONDAY);
        serviceC.setCalendar(serviceCalendarC);

        routeA.addService(serviceA);
        ferryRoute.addService(serviceB);
        routeC.addService(serviceC);

        container.addRoute(routeA);
        container.addRoute(ferryRoute);
        container.addRoute(routeC);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        MutableTrip tripA = new MutableTrip(StringIdFor.createId(MixedTransportTestData.TRIP_A_ID), "headSign",
                serviceA, routeA, routeA.getTransportMode());
        serviceA.addTrip(tripA);

        MutableStation first = StationHelper.forTestMutable(MixedTransportTestData.FIRST_STATION, "area1", "startStation",
                nearAltrincham, dataSourceID);
        addAStation(container, first);
        addRouteStation(container, first, routeA);
        NoPlatformStopCall stopA = createStop(tripA, first, TramTime.of(8, 0),
                TramTime.of(8, 0), 1);
        tripA.addStop(stopA);

        MutableStation second = StationHelper.forTestMutable(MixedTransportTestData.SECOND_STATION, "area2",
                "secondStation", nearPiccGardens, dataSourceID);
        addAStation(container, second);
        addRouteStation(container, second, routeA);
        NoPlatformStopCall stopB = createStop(tripA, second, TramTime.of(8, 11),
                TramTime.of(8, 11), 2);
        tripA.addStop(stopB);

        MutableStation interchangeStation = StationHelper.forTestMutable(MixedTransportTestData.INTERCHANGE, "area3"
                , "cornbrookStation", nearShudehill, dataSourceID);
        addAStation(container, interchangeStation);
        addRouteStation(container, interchangeStation, routeA);
        NoPlatformStopCall stopC = createStop(tripA, interchangeStation, TramTime.of(8, 20),
                TramTime.of(8, 20), 3);
        tripA.addStop(stopC);

        MutableStation last =  StationHelper.forTestMutable(MixedTransportTestData.LAST_STATION, "area4",
                "endStation", nearPiccGardens, dataSourceID);
        addAStation(container, last);
        addRouteStation(container, last, routeA);
        NoPlatformStopCall stopD = createStop(tripA, last, TramTime.of(8, 40),
                TramTime.of(8, 40), 4);
        tripA.addStop(stopD);

        // service A
        routeA.addTrip(tripA);

        MutableStation stationFour = StationHelper.forTestMutable(MixedTransportTestData.STATION_FOUR, "area4",
                "Station4", nearPiccGardens, dataSourceID);
        addAStation(container, stationFour);

        MutableStation stationFive = StationHelper.forTestMutable(MixedTransportTestData.STATION_FIVE, "area5",
                 "Station5", nearStockportBus, dataSourceID);
        addAStation(container, stationFive);

        //
        MutableTrip tripC = new MutableTrip(StringIdFor.createId("tripCId"), "headSignC",
                serviceC, routeC, routeC.getTransportMode());
        serviceC.addTrip(tripC);
        NoPlatformStopCall stopG = createStop(tripC, interchangeStation, TramTime.of(8, 26),
                TramTime.of(8, 27), 1);
        addRouteStation(container, interchangeStation, routeC);
        NoPlatformStopCall stopH = createStop(tripC, stationFive, TramTime.of(8, 31),
                TramTime.of(8, 33), 2);
        addRouteStation(container, stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        routeC.addTrip(tripC);

        // INTERCHANGE -> STATION_FOUR
        addRouteStation(container, stationFour, ferryRoute);
        addRouteStation(container, interchangeStation, ferryRoute);

        createInterchangeToStation4Trip(container,ferryRoute, serviceB, interchangeStation, stationFour,
                LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(container,ferryRoute, serviceB, interchangeStation, stationFour,
                LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(container,ferryRoute, serviceB, interchangeStation, stationFour,
                LocalTime.of(9, 20), "tripB3Id");

        container.addTrip(tripA);
        container.addTrip(tripC);

        container.addService(serviceA);
        container.addService(serviceB);
        container.addService(serviceC);
    }

    private void addAStation(TransportDataContainer container, MutableStation station) {
        container.addStation(station);
    }

    private static void addRouteStation(TransportDataContainer container, MutableStation station, Route route) {
        RouteStation routeStation = new RouteStation(station, route);
        container.addRouteStation(routeStation);

        station.addRouteDropOff(route);
        station.addRouteDropOff(route);

        station.addRoutePickUp(route);
        station.addRoutePickUp(route);
    }

    private static void createInterchangeToStation4Trip(TransportDataContainer container, MutableRoute route, MutableService service,
                                                        Station interchangeStation, Station station, LocalTime startTime, String tripId) {
        MutableTrip trip = new MutableTrip(StringIdFor.createId(tripId), "headSignTripB2", service, route, route.getTransportMode());
        service.addTrip(trip);
        NoPlatformStopCall stop1 = createStop(trip, interchangeStation, TramTime.ofHourMins(startTime),
                TramTime.ofHourMins(startTime.plusMinutes(5)), 1);
        trip.addStop(stop1);
        NoPlatformStopCall stop2 = createStop(trip, station, TramTime.ofHourMins(startTime.plusMinutes(5)),
                TramTime.ofHourMins(startTime.plusMinutes(8)), 2);
        trip.addStop(stop2);
        route.addTrip(trip);
        container.addTrip(trip);
    }

    private static NoPlatformStopCall createStop(MutableTrip trip, Station station,
                                                 TramTime arrivalTime, TramTime departureTime, int sequenceNum) {
        return new NoPlatformStopCall(station, arrivalTime, departureTime, sequenceNum, Regular, Regular, trip);
    }


    public static class MixedTransportTestData extends TransportDataContainer {

        private static final IdFor<Service> serviceAId = StringIdFor.createId("serviceAId");
        private static final IdFor<Service> serviceBId = StringIdFor.createId("serviceBId");
        private static final IdFor<Service> serviceCId = StringIdFor.createId("serviceCId");

        public static final String TRIP_A_ID = "tripAId";
        public static final String PREFIX = "XXX";
        public static final String FIRST_STATION = PREFIX + ":ST:FIRST";
        public static final String SECOND_STATION = PREFIX + ":ST:SECOND";
        public static final String LAST_STATION = PREFIX + ":ST:LAST";
        public static final String INTERCHANGE = PREFIX + ":INTERCHANGE";
        public static final String STATION_FOUR = PREFIX + ":ST:FOUR";
        public static final String STATION_FIVE = PREFIX + ":ST:FIVE";

        public MixedTransportTestData(ProvidesNow providesNow) {
            super(providesNow, "MixedTransportTestData");
        }

        @Override
        public void addStation(MutableStation station) {
            super.addStation(station);
        }

        public Station getFirst() {
            return getStationById(StringIdFor.createId(FIRST_STATION));
        }

        public Station getSecond() {
            return getStationById(StringIdFor.createId(SECOND_STATION));
        }

        public Station getLast() {
            return getStationById(StringIdFor.createId(LAST_STATION));
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
            result.put(DataSourceID.unknown, info);
            return result;
        }

    }
}
