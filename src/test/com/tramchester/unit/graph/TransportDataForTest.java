package com.tramchester.unit.graph;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.input.TramInterchanges;
import com.tramchester.domain.input.Stop;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportData;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static com.tramchester.domain.Station.METROLINK_PREFIX;
import static java.lang.String.format;

public class TransportDataForTest implements TransportData, StationRepository, PlatformRepository {
    private String serviceAId = "serviceAId";
    private String serviceBId = "serviceBId";

    public static final String FIRST_STATION = METROLINK_PREFIX+"_FIRST";
    public static final String SECOND_STATION = METROLINK_PREFIX+"_SECOND";
    public static final String LAST_STATION = METROLINK_PREFIX+"_LAST";
    public static final String INTERCHANGE = TramInterchanges.CORNBROOK;
    public static final String STATION_FOUR = METROLINK_PREFIX+"_FOUR";

    private Map<String, Station> stationMap = new HashMap<>();

    private Collection<Route> routes;
    private Map<String,Platform> platforms;

    public TransportDataForTest() throws TramchesterException {
        routes = new LinkedList<>();
        platforms = new HashMap<>();
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

        LocalDate startDate = LocalDate.of(2014, 02, 10);
        LocalDate endDate = LocalDate.of(2020, 8, 15);
        serviceA.setServiceDateRange(startDate, endDate);
        serviceB.setServiceDateRange(startDate, endDate);

        // 8*60=480

        // tripA: FIRST_STATION -> SECOND_STATION -> INTERCHANGE -> LAST_STATION
        Trip tripA = new Trip("trip1Id", "headSign", serviceAId, routeA.getId());

        double latitude = 180.00;
        double longitude = 270.0;
        LatLong latLong = new LatLong(latitude, longitude);
        Station first = new Station(FIRST_STATION, "area", "startStation", latLong, true);
        addStation(first);
        Stop stopA = createStop(first, TramTime.create(8, 0), TramTime.create(8, 0), routeA.getId(), serviceAId);
        tripA.addStop(stopA);

        Station second = new Station(SECOND_STATION, "area", "secondStation", latLong, true);
        Stop stopB = createStop(second, TramTime.create(8, 11), TramTime.create(8, 11), routeA.getId(), serviceAId);
        tripA.addStop(stopB);
        addStation(second);

        Station interchangeStation = new Station(INTERCHANGE, "area", "cornbrook", latLong, true);
        Stop stopC = createStop(interchangeStation, TramTime.create(8, 20), TramTime.create(8, 20), routeA.getId(), serviceAId);
        tripA.addStop(stopC);
        addStation(interchangeStation);

        Station last = new Station(LAST_STATION, "area", "endStation", latLong, true);
        addStation(last);
        Stop stopD = createStop(last, TramTime.create(8, 40), TramTime.create(8, 40), routeA.getId(), serviceAId);
        tripA.addStop(stopD);
        // service
        serviceA.addTrip(tripA);

        // tripB: INTERCHANGE -> STATION_FOUR
        Trip tripB = new Trip("trip2Id", "headSign", serviceBId, routeB.getId());
        Stop stopE = createStop(interchangeStation, TramTime.create(8, 26), TramTime.create(8, 26), routeB.getId(), serviceBId);
        tripB.addStop(stopE);

        Station four = new Station(STATION_FOUR, "area", "stat4Station", new LatLong(170.00, 160.00), true);
        addStation(four);
        Stop stopF = createStop(four, TramTime.create(8, 36), TramTime.create(8, 36), routeB.getId(), serviceBId);
        tripB.addStop(stopF);
        // service
        serviceB.addTrip(tripB);
    }

    private void addStation(Station station) {
        stationMap.put(station.getId(), station);
    }

    private Stop createStop(Location startStation, TramTime arrivalTime, TramTime departureTime, String routeId, String serviceId) {
        String platformId = startStation.getId() + "1";
        platforms.put(platformId, new Platform(platformId, format("%s platform 1", startStation.getName())));
        return new Stop(platformId, startStation, arrivalTime, departureTime, routeId, serviceId);
    }

    @Override
    public Collection<Route> getRoutes() {
        return routes;
    }

    @Override
    public Stream<Trip> getTripsByRouteId(String routeId) {
        return null;
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
}
