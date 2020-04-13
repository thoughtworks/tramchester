package com.tramchester.unit.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.input.TramStopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.time.TramServiceDate;
import com.tramchester.domain.time.TramTime;
import com.tramchester.testSupport.RoutesForTesting;
import com.tramchester.testSupport.Stations;
import com.tramchester.repository.TransportDataSource;
import com.tramchester.testSupport.TestEnv;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.Station.METROLINK_PREFIX;
import static java.lang.String.format;

public class TransportDataForTest implements TransportDataSource {
    private final Service serviceB;
    private String serviceAId = "serviceAId";
    private String serviceBId = "serviceBId";
    private String serviceCId = "serviceCId";

    public static final String FIRST_STATION = METROLINK_PREFIX+"_ST_FIRST";
    public static final String SECOND_STATION = METROLINK_PREFIX+"_ST_SECOND";
    public static final String LAST_STATION = METROLINK_PREFIX+"_ST_LAST";
    public static final String INTERCHANGE = Stations.Cornbrook.getId();
    public static final String STATION_FOUR = METROLINK_PREFIX+"_ST_FOUR";
    public static final String STATION_FIVE = METROLINK_PREFIX+"_ST_FIVE";

    private Map<String, Station> stationIdMap = new HashMap<>();
    private Map<String, Station> stationNameMap = new HashMap<>();

    private Map<String,Platform> platforms;
    private Map<String, Route> routes;
    private Map<String, RouteStation> routeStations;
    private Set<Service> services;
    private Map<String, Trip> trips;

    public TransportDataForTest() {
        double latitude = 180.00;
        double longitude = 270.0;

        routes = new HashMap<>();
        services = new HashSet<>();
        platforms = new HashMap<>();
        trips = new HashMap<>();
        routeStations = new HashMap<>();

        Route routeA = RoutesForTesting.ALTY_TO_BURY; //new Route(RoutesForTesting.ALTY_TO_BURY, "routeACode", "routeA", agency, TransportMode.Tram);
        Route routeB = RoutesForTesting.ROCH_TO_DIDS; //new Route(RoutesForTesting.ROCH_TO_DIDS, "routeBCode", "routeB", agency, TransportMode.Tram);
        Route routeC = RoutesForTesting.DIDS_TO_ROCH; //new Route(RoutesForTesting.DIDS_TO_ROCH, "routeCCode", "routeC", agency, TransportMode.Tram);

        routes.put(routeA.getId(), routeA);
        routes.put(routeB.getId(), routeB);
        routes.put(routeC.getId(), routeC);

        Service serviceA = new Service(serviceAId, routeA);
        serviceB = new Service(serviceBId, routeB);
        Service serviceC = new Service(serviceCId, routeC);

        routeA.addService(serviceA);
        routeB.addService(serviceB);
        routeC.addService(serviceC);

        serviceA.setDays(true, false, false, false, false, false, false);
        serviceB.setDays(true, false, false, false, false, false, false);
        serviceC.setDays(true, false, false, false, false, false, false);

        LocalDate startDate = LocalDate.of(2014, 02, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);
        serviceA.setServiceDateRange(startDate, endDate);
        serviceB.setServiceDateRange(startDate, endDate);
        serviceC.setServiceDateRange(startDate, endDate);

        services.add(serviceA);
        services.add(serviceB);
        services.add(serviceC);

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        Trip tripA = new Trip("tripAId", "headSign", serviceA, routeA);

        LatLong latLong = new LatLong(latitude, longitude);
        Station first = new Station(FIRST_STATION, "area1", "startStation", latLong, true);
        addStation(first);
        addRouteStation(first, routeA);
        TramStopCall stopA = createStop(first, TramTime.of(8, 0), TramTime.of(8, 0), 1);
        tripA.addStop(stopA);

        Station second = new Station(SECOND_STATION, "area2", "secondStation", latLong, true);
        TramStopCall stopB = createStop(second, TramTime.of(8, 11), TramTime.of(8, 11), 2);
        tripA.addStop(stopB);
        addStation(second);
        addRouteStation(second, routeA);

        Station interchangeStation = new Station(INTERCHANGE, "area3", "cornbrookStation", latLong, true);
        TramStopCall stopC = createStop(interchangeStation, TramTime.of(8, 20), TramTime.of(8, 20), 3);
        tripA.addStop(stopC);
        addStation(interchangeStation);
        addRouteStation(interchangeStation, routeA);

        Station last = new Station(LAST_STATION, "area4", "endStation", latLong, true);
        addStation(last);
        addRouteStation(last, routeA);
        TramStopCall stopD = createStop(last, TramTime.of(8, 40), TramTime.of(8, 40), 4);
        tripA.addStop(stopD);
        // service
        serviceA.addTrip(tripA);

        Station stationFour = new Station(STATION_FOUR, "area4", "Station4", new LatLong(170.00, 160.00), true);
        addStation(stationFour);

        Station stationFive = new Station(STATION_FIVE, "area5", "Station5", new LatLong(170.00, 160.00), true);
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
    }

    private void addRouteStation(Station station, Route route) {
        RouteStation routeStation = new RouteStation(station, route);
        routeStations.put(routeStation.getId(), routeStation);
    }

    private void addTrip(Trip trip) {
        trips.put(trip.getId(),trip);
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

    private void addStation(Station station) {
        stationIdMap.put(station.getId(), station);
        stationNameMap.put(station.getName().toLowerCase(), station);
    }

    private TramStopCall createStop(Station startStation, TramTime arrivalTime, TramTime departureTime, int sequenceNum) {
        String platformId = startStation.getId() + "1";
        Platform platform = new Platform(platformId, format("%s platform 1", startStation.getName()));
        platforms.put(platformId, platform);
        return new TramStopCall(platform, startStation, (byte) sequenceNum, arrivalTime, departureTime);
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
    public Stream<Trip> getTripsByRoute(Route route) {
        throw new RuntimeException("Not implemented");
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
        Agency agency = new Agency("MET");
        routes.values().forEach(agency::addRoute);
        return Arrays.asList(agency);
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
        return new FeedInfo("publisherName", "publisherUrl", "timezone", "lang", LocalDate.of(2016,5,25),
                LocalDate.of(2016,6,30), "version");
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
    public List<AreaDTO> getAreas() {
        throw new RuntimeException("Not implemented");
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
