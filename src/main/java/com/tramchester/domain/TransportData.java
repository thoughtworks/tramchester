package com.tramchester.domain;

import com.tramchester.dataimport.data.*;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TransportData {
    private HashMap<String, Trip> trips = new HashMap<>();
    private HashMap<String, Station> stations = new HashMap<>();
    private HashMap<String, Service> services = new HashMap<>();
    private HashMap<String, Route> routes = new HashMap<>();


    public TransportData(List<StopData> stopDataList, List<RouteData> routeDataList, List<TripData> tripDataList, List<StopTimeData> stopTimeDataList, List<CalendarData> calendarDataList) {
        for (StopData stopData : stopDataList) {
            if (!stations.keySet().contains(stopData.getId())) {
                stations.put(stopData.getId(), new Station(stopData.getId(), stopData.getCode(), stopData.getName(), stopData.getLatitude(), stopData.getLongitude()));
            }
        }

        for (RouteData routeData : routeDataList) {
            Route route = new Route(routeData.getId(), routeData.getCode(), routeData.getName());
            routes.put(route.getId(), route);
        }

        for (TripData tripData : tripDataList) {
            Trip trip = getTrip(tripData.getTripId());
            Service service = getService(tripData.getServiceId());
            Route route = routes.get(tripData.getRouteId());
            if (route != null) {
                service.addTrip(trip);
                route.addService(service);
            }
        }

        for (StopTimeData stopTimeData : stopTimeDataList) {
            Trip trip = getTrip(stopTimeData.getTripId());

            Stop stop = new Stop(stopTimeData.getArrivalTime(),
                    stopTimeData.getDepartureTime(),
                    stations.get(stopTimeData.getStopId()),
                    stopTimeData.getStopSequence(),
                    getStopType(stopTimeData),
                    stopTimeData.getMinutesFromMidnight());

            trip.addStop(stop);
        }

        for (CalendarData calendar : calendarDataList) {
            Service service = services.get(calendar.getServiceId());

            if (service != null) {
                if (calendar.getStart().equals(new DateTime(2015, 01, 05, 0, 0, 0))) {
                    service.setDays(
                            calendar.isMonday(),
                            calendar.isTuesday(),
                            calendar.isWednesday(),
                            calendar.isThursday(),
                            calendar.isFriday(),
                            calendar.isSaturday(),
                            calendar.isSunday()
                    );
                } else {
                    services.remove(calendar.getServiceId());
                }
            }
        }

    }


    private Service getService(String serviceId) {
        if (!services.keySet().contains(serviceId)) {
            services.put(serviceId, new Service(serviceId));
        }
        return services.get(serviceId);
    }

    public HashMap<String, Route> getRoutes() {
        return routes;
    }

    private StopType getStopType(StopTimeData stopTimeData) {
        if (stopTimeData.getPickupType().equals("0") && stopTimeData.getDropOffType().equals("1")) {
            return StopType.START;
        } else if (stopTimeData.getPickupType() == "1" && stopTimeData.getDropOffType() == "0") {
            return StopType.END;
        }
        return StopType.MIDDLE;

    }

    private Trip getTrip(String tripId) {
        if (!trips.keySet().contains(tripId)) {
            trips.put(tripId, new Trip(tripId));
        }
        return trips.get(tripId);
    }

    public List<Station> getStations() {
        ArrayList<Station> stationList = new ArrayList<>();
        stationList.addAll(stations.values());
        return stationList;
    }

    public Station getStation(String stationId) {
        return stations.get(stationId);
    }

    public List<ServiceTime> getTimes(String serviceId, String stationId, int minutesFromMidnight) {
        List<ServiceTime> serviceTimes = new ArrayList<>();
        Service service = services.get(serviceId);
        List<Trip> tripsAfter = service.getTripsAfter(stationId, minutesFromMidnight);
        for (Trip trip : tripsAfter) {
            Stop stop = trip.getStop(stationId);
            serviceTimes.add(new ServiceTime(stop.getDepartureTime(), stop.getArrivalTime(), "route", serviceId, "headsign", 4));
        }
        return serviceTimes;
    }
}
