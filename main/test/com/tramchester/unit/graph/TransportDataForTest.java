package com.tramchester.unit.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.StationLocations;
import com.tramchester.repository.TransportDataSource;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.testSupport.TestEnv;
import org.picocontainer.Disposable;
import org.picocontainer.Startable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static com.tramchester.domain.places.Station.METROLINK_PREFIX;
import static java.lang.String.format;

@SuppressWarnings("FieldCanBeLocal")
public class TransportDataForTest implements TransportDataSource, Startable, Disposable {
    private Service serviceB;
    private final String serviceAId = "serviceAId";
    private final String serviceBId = "serviceBId";
    private final String serviceCId = "serviceCId";

    public static final String FIRST_STATION = METROLINK_PREFIX + "_ST_FIRST";
    public static final String SECOND_STATION = METROLINK_PREFIX + "_ST_SECOND";
    public static final String LAST_STATION = METROLINK_PREFIX + "_ST_LAST";
    public static final String INTERCHANGE = Stations.Cornbrook.getId();
    private static final String STATION_FOUR = METROLINK_PREFIX + "_ST_FOUR";
    private static final String STATION_FIVE = METROLINK_PREFIX + "_ST_FIVE";

    private final Map<String, Station> stationIdMap = new HashMap<>();
    private final Map<String, Station> stationNameMap = new HashMap<>();

    private final Map<String, Platform> platforms;
    private final Map<String, Route> routes;
    private final Map<String, RouteStation> routeStations;
    private final Set<Service> services;
    private final Map<String, Trip> trips;

    private final StationLocations stationLocations;
    private Agency agency;

    public TransportDataForTest(StationLocations stationLocations) {
        this.stationLocations = stationLocations;

        routes = new HashMap<>();
        services = new HashSet<>();
        platforms = new HashMap<>();
        trips = new HashMap<>();
        routeStations = new HashMap<>();
    }

    @Override
    public void dispose() {
        routes.clear();
        services.clear();
        platforms.clear();
        trips.clear();
        routeStations.clear();
    }

    @Override
    public void start() {
        populateTestData();
    }

    @Override
    public void stop() {

    }

    private void populateTestData() {
        Route routeA = RoutesForTesting.ALTY_TO_BURY; // TODO This route not present during lockdown
        Route routeB = RoutesForTesting.ROCH_TO_DIDS;
        Route routeC = RoutesForTesting.DIDS_TO_ROCH;

        routes.put(routeA.getId(), routeA);
        routes.put(routeB.getId(), routeB);
        routes.put(routeC.getId(), routeC);

        agency = new Agency("MET");
        routes.values().forEach(agency::addRoute);

        Service serviceA = new Service(serviceAId, routeA);
        serviceB = new Service(serviceBId, routeB);
        Service serviceC = new Service(serviceCId, routeC);

        routeA.addService(serviceA);
        routeB.addService(serviceB);
        routeC.addService(serviceC);

        serviceA.setDays(true, false, false, false, false, false, false);
        serviceB.setDays(true, false, false, false, false, false, false);
        serviceC.setDays(true, false, false, false, false, false, false);

        LocalDate startDate = LocalDate.of(2014, 2, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);
        serviceA.setServiceDateRange(startDate, endDate);
        serviceB.setServiceDateRange(startDate, endDate);
        serviceC.setServiceDateRange(startDate, endDate);

        services.add(serviceA);
        services.add(serviceB);
        services.add(serviceC);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        Trip tripA = new Trip("tripAId", "headSign", serviceA, routeA);

        //LatLong latLong = new LatLong(latitude, longitude);
        Station first = new Station(FIRST_STATION, "area1", "startStation", TestEnv.nearAltrincham, true);
        addStation(first);
        addRouteStation(first, routeA);
        TramStopCall stopA = createStop(first, TramTime.of(8, 0), TramTime.of(8, 0), 1);
        tripA.addStop(stopA);

        Station second = new Station(SECOND_STATION, "area2", "secondStation", TestEnv.nearPiccGardens, true);
        addStation(second);
        addRouteStation(second, routeA);
        TramStopCall stopB = createStop(second, TramTime.of(8, 11), TramTime.of(8, 11), 2);
        tripA.addStop(stopB);

        Station interchangeStation = new Station(INTERCHANGE, "area3", "cornbrookStation", TestEnv.nearShudehill, true);
        addStation(interchangeStation);
        addRouteStation(interchangeStation, routeA);
        TramStopCall stopC = createStop(interchangeStation, TramTime.of(8, 20), TramTime.of(8, 20), 3);
        tripA.addStop(stopC);

        Station last = new Station(LAST_STATION, "area4", "endStation", TestEnv.nearPiccGardens, true);
        addStation(last);
        addRouteStation(last, routeA);
        TramStopCall stopD = createStop(last, TramTime.of(8, 40), TramTime.of(8, 40), 4);
        tripA.addStop(stopD);

        // service A
        serviceA.addTrip(tripA);

        Station stationFour = new Station(STATION_FOUR, "area4", "Station4", TestEnv.nearPiccGardens, true);
        addStation(stationFour);

        Station stationFive = new Station(STATION_FIVE, "area5", "Station5", TestEnv.nearStockportBus, true);
        addStation(stationFive);

        //
        Trip tripC = new Trip("tripCId", "headSignC", serviceC, routeC);
        TramStopCall stopG = createStop(interchangeStation, TramTime.of(8, 26), TramTime.of(8, 27), 1);
        addRouteStation(interchangeStation, routeC);
        TramStopCall stopH = createStop(stationFive, TramTime.of(8, 31), TramTime.of(8, 33), 2);
        addRouteStation(stationFive, routeC);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        serviceC.addTrip(tripC);

        // INTERCHANGE -> STATION_FOUR
        addRouteStation(stationFour, routeB);
        addRouteStation(interchangeStation, routeB);

        createInterchangeToStation4Trip(routeB, serviceB, interchangeStation, stationFour, LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(routeB, serviceB, interchangeStation, stationFour, LocalTime.of(9, 20), "tripB3Id");

        addTrip(tripA);
        addTrip(tripC);

        services.forEach(Service::updateTimings);
    }

    private void addRouteStation(Station station, Route route) {
        RouteStation routeStation = new RouteStation(station, route);
        routeStations.put(routeStation.getId(), routeStation);
        station.addRoute(route);
    }

    private void addTrip(Trip trip) {
        trips.put(trip.getId(), trip);
    }

    private void createInterchangeToStation4Trip(Route route, Service service, Station interchangeStation, Station station,
                                                 LocalTime startTime, String tripB2Id) {
        Trip trip = new Trip(tripB2Id, "headSignTripB2", serviceB, route);
        TramStopCall stop1 = createStop(interchangeStation, TramTime.of(startTime),
                TramTime.of(startTime.plusMinutes(5)), 1);
        trip.addStop(stop1);
        TramStopCall stop2 = createStop(station, TramTime.of(startTime.plusMinutes(5)),
                TramTime.of(startTime.plusMinutes(8)), 2);
        trip.addStop(stop2);
        service.addTrip(trip);
        addTrip(trip);
    }

    public void addStation(Station station) {
        stationIdMap.put(station.getId(), station);
        stationNameMap.put(station.getName().toLowerCase(), station);
        stationLocations.addStation(station);
    }

    private TramStopCall createStop(Station station, TramTime arrivalTime, TramTime departureTime, int sequenceNum) {
        String platformId = station.getId() + "1";
        Platform platform = new Platform(platformId, format("%s platform 1", station.getName()));
        platforms.put(platformId, platform);
        station.addPlatform(platform);
        return new TramStopCall(platform, station, (byte) sequenceNum, arrivalTime, departureTime);
    }

    @Override
    public Collection<Service> getServices() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<Service> getServicesOnDate(TramServiceDate date) {
        return services;
    }

    @Override
    public Collection<Route> getRoutes() {
        return routes.values();
    }

    @Override
    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    @Override
    public Set<Station> getStations() {
        return new HashSet<>(stationIdMap.values());
    }

    @Override
    public Collection<Agency> getAgencies() {

        return Collections.singletonList(agency);
    }

    @Override
    public Set<RouteStation> getRouteStations() {
        return new HashSet<>(routeStations.values());
    }

    @Override
    public RouteStation getRouteStation(String routeStationId) {
        return routeStations.get(routeStationId);
    }

    @Override
    public FeedInfo getFeedInfo() {
        return new FeedInfo("publisherName", "publisherUrl", "timezone", "lang",
                LocalDate.of(2016, 5, 25),
                LocalDate.of(2016, 6, 30), "version");
    }

    @Override
    public Station getStation(String stationId) {
        return stationIdMap.get(stationId);
    }

    @Override
    public boolean hasStationId(String stationId) {
        return stationIdMap.containsKey(stationId);
    }

    @Override
    public Optional<Station> getStationByName(String name) {
        return Optional.of(stationNameMap.get(name.toLowerCase()));
    }

    @Override
    public Optional<Platform> getPlatformById(String platformId) {
        return Optional.ofNullable(platforms.get(platformId));
    }

    @Override
    public Trip getTrip(String tripId) {
        return trips.get(tripId);
    }

    @Override
    public Collection<Trip> getTrips() {
        return trips.values();
    }

    public Station getFirst() {
        return stationIdMap.get(FIRST_STATION);
    }

    public Station getSecond() {
        return stationIdMap.get(SECOND_STATION);
    }

    public Station getInterchange() {
        return stationIdMap.get(INTERCHANGE);
    }

    public Station getSecondStation() {
        return stationIdMap.get(SECOND_STATION);
    }

    public Station getLast() {
        return stationIdMap.get(LAST_STATION);
    }

    public Station getFifthStation() {
        return stationIdMap.get(STATION_FIVE);
    }

    public Station getFourthStation() {
        return stationIdMap.get(STATION_FOUR);
    }

}
