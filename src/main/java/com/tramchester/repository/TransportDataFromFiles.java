package com.tramchester.repository;

import com.tramchester.dataimport.data.*;
import com.tramchester.domain.*;
import com.tramchester.domain.presentation.ServiceTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class TransportDataFromFiles implements TransportData {
    private static final Logger logger = LoggerFactory.getLogger(TransportDataFromFiles.class);
    private HashMap<String, Trip> trips = new HashMap<>();        // trip id -> trip
    private HashMap<String, Station> stations = new HashMap<>();  // station id -> station
    private HashMap<String, Service> services = new HashMap<>();  // service id -> service
    private HashMap<String, Route> routes = new HashMap<>();      // route id -> route
    private FeedInfo feedInfo = null;

    public TransportDataFromFiles(Stream<StopData> stops, Stream<RouteData> routes, Stream<TripData> trips,
                                  Stream<StopTimeData> stopTimes, Stream<CalendarData> calendars,
                                  Stream<FeedInfo> feedInfo)  {
        logger.info("Loading transport data from files");
        Optional<FeedInfo> maybeFeedInfo = feedInfo.limit(1).findFirst();
        if (maybeFeedInfo.isPresent()) {
            this.feedInfo = maybeFeedInfo.get();
        } else {
            logger.warn("Did not find feedinfo");
        }

        stops.forEach((stop) -> {
            String stopId = stop.getId();
            String stationId = Station.formId(stopId);
            if (!stations.keySet().contains(stationId)) {
                Station station = new Station(stopId, stop.getArea(), stop.getName(),
                        stop.getLatitude(), stop.getLongitude(), stop.isTram());
                stations.put(stationId, station);
            }
        });

        routes.forEach((routeData) -> {
            Route route = new Route(routeData.getId(), routeData.getCode(), routeData.getName(), routeData.getAgency());
            this.routes.put(route.getId(), route);
        });

        trips.forEach((tripData) -> {
            Trip trip = getOrCreateTrip(tripData.getTripId(), tripData.getTripHeadsign(), tripData.getServiceId());
            Service service = getOrInsertService(tripData.getServiceId(), tripData.getRouteId());
            Route route = this.routes.get(tripData.getRouteId());
            if (route != null) {
                service.addTrip(trip);
                route.addService(service);
            }
        });

        stopTimes.forEach((stopTimeData) -> {
            Trip trip = getTrip(stopTimeData.getTripId());

            String stopId = stopTimeData.getStopId();
            String stationId = Station.formId(stopId);
            if (!stations.containsKey(stationId)) {
                logger.error("Cannot find station for Id " + stationId);
            }
            Stop stop = new Stop(stations.get(stationId), stopTimeData.getArrivalTime(),
                    stopTimeData.getDepartureTime()
            );

            trip.addStop(stop);
        });

        calendars.forEach((calendar) -> {
            Service service = services.get(calendar.getServiceId());

            if (service != null) {
                service.setDays(
                        calendar.isMonday(),
                        calendar.isTuesday(),
                        calendar.isWednesday(),
                        calendar.isThursday(),
                        calendar.isFriday(),
                        calendar.isSaturday(),
                        calendar.isSunday()
                );
                service.setServiceDateRange(calendar.getStartDate(), calendar.getEndDate());
            }
        });

        // update svcs where calendar data is missing
        services.values().stream().filter(svc -> svc.getDays().get(DaysOfWeek.Monday) == null).forEach(svc -> {
            logger.warn(String.format("Service %s is missing calendar information", svc.getServiceId()));
            svc.setDays(false, false, false, false, false, false, false);
        });
        logger.info("Data load is complete");

    }

    private Trip getOrCreateTrip(String tripId, String tripHeadsign, String serviceId) {
        if (!trips.keySet().contains(tripId)) {
            trips.put(tripId, new Trip(tripId, tripHeadsign, serviceId));
        }
        return trips.get(tripId);
    }


    private Service getOrInsertService(String serviceId, String routeId) {
        if (!services.keySet().contains(serviceId)) {
            services.put(serviceId, new Service(serviceId, routeId));
        }
        return services.get(serviceId);
    }

    public Collection<Route> getRoutes() {
        return routes.values();
    }

    @Override
    public Route getRoute(String routeId) {
        return routes.get(routeId);
    }

    private Trip getTrip(String tripId) {
        return trips.get(tripId);
    }

    public List<Station> getStations() {
        ArrayList<Station> stationList = new ArrayList<>();
        stationList.addAll(stations.values());
        return stationList;
    }

    @Override
    public FeedInfo getFeedInfo() {
        return feedInfo;
    }

    public Station getStation(String stationId) {
        return stations.get(stationId);
    }

    public List<ServiceTime> getTimes(String serviceId, String firstStationId, String lastStationId,
                                      int minutesFromMidnight, int maxNumberOfTrips) {
        logger.info(String.format("Get times for service %s from %s to %s at minutes past %s",
                serviceId, firstStationId, lastStationId, minutesFromMidnight));
        List<ServiceTime> serviceTimes = new ArrayList<>();
        Service service = getServiceById(serviceId);

        List<Trip> tripsAfter = service.getTripsAfter(firstStationId, lastStationId, minutesFromMidnight,
                maxNumberOfTrips);

        for (Trip trip : tripsAfter) {
            List<ServiceTime> times = trip.getServiceTimes(firstStationId, lastStationId, minutesFromMidnight);
            serviceTimes.addAll(times);
        }
        return serviceTimes;
    }

    public Service getServiceById(String svcId) {
        if (!services.containsKey(svcId)) {
            logger.error("Unable to find service with id: " + svcId);
            throw new NoSuchElementException("Unable to find service " + svcId);
        }
        return services.get(svcId);
    }

    public Collection<Service> getServices() {
        return services.values();
    }

    public Set<Trip> getTripsFor(String stationId) {
        Set<Trip> callingTrips = new HashSet<>();
        trips.values().forEach(trip -> {
            if (trip.callsAt(stationId)) {
                callingTrips.add(trip);
            }
        });
        return callingTrips;
    }
}
