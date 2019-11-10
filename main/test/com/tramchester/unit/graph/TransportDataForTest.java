package com.tramchester.unit.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.DTO.AreaDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.integration.Stations;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;
import com.tramchester.repository.TransportDataSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.Station.METROLINK_PREFIX;
import static java.lang.String.format;

public class TransportDataForTest implements TransportDataSource {
    private String serviceAId = "serviceAId";
    private String serviceBId = "serviceBId";
    private String serviceCId = "serviceCId";

    public static final String FIRST_STATION = METROLINK_PREFIX+"_ST_FIRST";
    public static final String SECOND_STATION = METROLINK_PREFIX+"_ST_SECOND";
    public static final String LAST_STATION = METROLINK_PREFIX+"_ST_LAST";
    public static final String INTERCHANGE = Stations.Cornbrook.getId();
    public static final String STATION_FOUR = METROLINK_PREFIX+"_ST_FOUR";

    private Map<String, Station> stationMap = new HashMap<>();
    private Map<String,Platform> platforms;
    private Collection<Route> routes;
    private Set<Service> services;

    public TransportDataForTest() throws TramchesterException {
        double latitude = 180.00;
        double longitude = 270.0;

        routes = new LinkedList<>();
        services = new HashSet<>();
        platforms = new HashMap<>();
        Route routeA = new Route("routeAId", "routeACode", "routeA", "MET");
        Route routeB = new Route("routeBId", "routeBCode", "routeB", "MET");

        routes.add(routeA);
        routes.add(routeB);

        Service serviceA = new Service(serviceAId, routeA.getId());
        routeA.addService(serviceA);

        Service serviceB = new Service(serviceBId, routeB.getId());
        Service serviceC = new Service(serviceCId, routeB.getId());
        routeB.addService(serviceB);
        routeB.addService(serviceC);

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
        Trip tripA = new Trip("tripAId", "headSign", serviceAId, routeA.getId());

        LatLong latLong = new LatLong(latitude, longitude);
        Station first = new Station(FIRST_STATION, "area1", "startStation", latLong, true);
        addStation(first);
        Stop stopA = createStop(first, TramTime.create(8, 0), TramTime.create(8, 0), routeA.getId(), serviceAId, 1);
        tripA.addStop(stopA);

        Station second = new Station(SECOND_STATION, "area2", "secondStation", latLong, true);
        Stop stopB = createStop(second, TramTime.create(8, 11), TramTime.create(8, 11), routeA.getId(), serviceAId, 2);
        tripA.addStop(stopB);
        addStation(second);

        Station interchangeStation = new Station(INTERCHANGE, "area3", "cornbrookStation", latLong, true);
        Stop stopC = createStop(interchangeStation, TramTime.create(8, 20), TramTime.create(8, 20), routeA.getId(), serviceAId, 3);
        tripA.addStop(stopC);
        addStation(interchangeStation);

        Station last = new Station(LAST_STATION, "area4", "endStation", latLong, true);
        addStation(last);
        Stop stopD = createStop(last, TramTime.create(8, 40), TramTime.create(8, 40), routeA.getId(), serviceAId, 4);
        tripA.addStop(stopD);
        // service
        serviceA.addTrip(tripA);

        // INTERTERCHANGE -> LAST_STATION
        Trip tripC = new Trip("tripCId", "headSign", serviceAId, routeA.getId());
        Stop stopG = createStop(interchangeStation, TramTime.create(9, 05), TramTime.create(9, 15), routeB.getId(), serviceCId, 1);
        Stop stopH = createStop(last, TramTime.create(9, 16), TramTime.create(9, 20), routeB.getId(), serviceCId, 2);
        tripC.addStop(stopG);
        tripC.addStop(stopH);
        serviceC.addTrip(tripC);

        Station four = new Station(STATION_FOUR, "area5", "Station4", new LatLong(170.00, 160.00), true);
        addStation(four);

        // INTERCHANGE -> STATION_FOUR
        createInterchangeToStation4Trip(routeB, serviceB, interchangeStation, four, LocalTime.of(8, 26), "tripBId");
        createInterchangeToStation4Trip(routeB, serviceB, interchangeStation, four, LocalTime.of(9, 10), "tripB2Id");
        createInterchangeToStation4Trip(routeB, serviceB, interchangeStation, four, LocalTime.of(9, 20), "tripB3Id");
    }

    public void createInterchangeToStation4Trip(Route routeB, Service serviceB, Station interchangeStation, Station four, LocalTime startTime, String tripB2Id) {
        Trip tripB2 = new Trip(tripB2Id, "headSignTripB2", serviceBId, routeB.getId());
        Stop stopE2 = createStop(interchangeStation, TramTime.of(startTime),
                TramTime.of(startTime.plusMinutes(5)), routeB.getId(), serviceBId, 1);
        tripB2.addStop(stopE2);
        Stop stopF2 = createStop(four, TramTime.of(startTime.plusMinutes(5)),
                TramTime.of(startTime.plusMinutes(8)), routeB.getId(), serviceBId, 2);
        tripB2.addStop(stopF2);
        serviceB.addTrip(tripB2);
    }

    private void addStation(Station station) {
        stationMap.put(station.getId(), station);
    }

    private Stop createStop(Location startStation, TramTime arrivalTime, TramTime departureTime, String routeId, String serviceId, int seuqenceId) {
        String platformId = startStation.getId() + "1";
        platforms.put(platformId, new Platform(platformId, format("%s platform 1", startStation.getName())));
        return new Stop(platformId, startStation, seuqenceId, arrivalTime, departureTime, routeId, serviceId);
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
        return routes;
    }

    @Override
    public Stream<Trip> getTripsByRouteId(String routeId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Route getRoute(String routeId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<Station> getStations() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public FeedInfo getFeedInfo() {
        return new FeedInfo("publisherName", "publisherUrl", "timezone", "lang", LocalDate.of(2016,5,25),
                LocalDate.of(2016,6,30), "version");
    }

    @Override
    public Optional<Station> getStation(String stationId) {
        return Optional.of(stationMap.get(stationId));
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
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Optional<ServiceTime> getFirstServiceTime(String serviceId, Location firstStation, Location lastStation, TimeWindow window) {
        throw new RuntimeException("Not implemented");
    }
}
