package com.tramchester.domain;

import com.tramchester.dataimport.data.*;
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

    public TransportDataFromFiles(Stream<StopData> stopDataList, Stream<RouteData> routeDataList, Stream<TripData> tripDataList,
                                  Stream<StopTimeData> stopTimeDataList, Stream<CalendarData> calendarDataList,
                                  Stream<FeedInfo> feedInfoData)  {
        Optional<FeedInfo> maybeFeedInfo = feedInfoData.limit(1).findFirst();
        if (maybeFeedInfo.isPresent()) {
            feedInfo = maybeFeedInfo.get();
        } else {
            logger.warn("Did not find feedinfo");

        }

        stopDataList.forEach((stopData) -> {
            String stopId = stopData.getId();
            String stationId = Station.formId(stopId);
            if (!stations.keySet().contains(stationId)) {
                Station station = new Station(stopId, stopData.getName(),
                        stopData.getLatitude(), stopData.getLongitude());
                logger.info("Add station with id " + stationId);
                stations.put(stationId, station);
            }
        } );

        routeDataList.forEach((routeData) -> {
            Route route = new Route(routeData.getId(), routeData.getCode(), routeData.getName(), routeData.getAgency());
            routes.put(route.getId(), route);
        } );

        tripDataList.forEach((tripData) -> {
            Trip trip = getOrCreateTrip(tripData.getTripId(), tripData.getTripHeadsign(), tripData.getServiceId());
            Service service = getOrInsertService(tripData.getServiceId(), tripData.getRouteId());
            Route route = routes.get(tripData.getRouteId());
            if (route != null) {
                service.addTrip(trip);
                route.addService(service);
            }
        });

        stopTimeDataList.forEach((stopTimeData) -> {
            Trip trip = getTrip(stopTimeData.getTripId());

            String stopId = stopTimeData.getStopId();
            String stationId = Station.formId(stopId);
            if (!stations.containsKey(stationId)) {
                logger.error("Cannot find station for Id " + stationId);
            }
            Stop stop = new Stop(stopTimeData.getArrivalTime(),
                    stopTimeData.getDepartureTime(),
                    stations.get(stationId),
                    stopTimeData.getMinutesFromMidnight());

            trip.addStop(stop);
        });

        calendarDataList.forEach((calendar) -> {
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
        } );

        // update svcs where calendar data is missing
        services.values().stream().filter(svc -> svc.getDays().get(DaysOfWeek.Monday) == null).forEach(svc -> {
            logger.warn(String.format("Service %s is missing calendar information", svc.getServiceId()));
            svc.setDays(false, false, false, false, false, false, false);
        });

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
        Service service = this.getService(serviceId);

        List<Trip> tripsAfter = service.getTripsAfter(firstStationId, lastStationId, minutesFromMidnight,
                maxNumberOfTrips);
        for (Trip trip : tripsAfter) {
            Stop firstStop = trip.getStop(firstStationId);
            Stop lastStop = trip.getStop(lastStationId);
            int fromMidnight = firstStop.getMinutesFromMidnight();

            ServiceTime serviceTime = new ServiceTime(firstStop.getDepartureTime(),
                    lastStop.getArrivalTime(), serviceId, trip.getHeadSign(), fromMidnight);

            logger.info(String.format("Add service time: %s ", serviceTime));
            serviceTimes.add(serviceTime);
        }
        return serviceTimes;
    }

    public Service getService(String svcId) {
        if (!services.containsKey(svcId)) {
            logger.error("Unable to find service with id: " + svcId);
            throw new NoSuchElementException("Unable to find service " + svcId);
        }
        return services.get(svcId);
    }

    public Collection<Service> getServices() {
        return services.values();
    }

    public List<Trip> getTripsFor(String stationId) {
        List<Trip> callingTrips = new LinkedList<>();
        trips.values().forEach(trip -> {
            if (trip.getStop(stationId)!=null) {
                callingTrips.add(trip);
            }
        });
        return callingTrips;
    }
}
