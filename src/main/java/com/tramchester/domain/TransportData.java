package com.tramchester.domain;

import com.tramchester.dataimport.StopTime;

import java.util.HashMap;
import java.util.List;

public class TransportData {
    private HashMap<String, Trip> trips = new HashMap<>();
    private HashMap<String, Station> stations = new HashMap<>();
    private HashMap<String, Service> services = new HashMap<>();
    private HashMap<String, Route> routes = new HashMap<>();


    public TransportData(List<StopData> stopDataList, List<RouteData> routeDataList, List<TripData> tripDataList, List<StopTime> stopTimes) {
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

        for (StopTime stopTime : stopTimes) {
            Trip trip = getTrip(stopTime.getTripId());
            Stop stop = new Stop(stopTime.getArrivalTime(), stopTime.getDepartureTime(), stations.get(stopTime.getStopId()), stopTime.getStopSequence(), getStopType(stopTime));
            trip.addStop(stop);
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

    private StopType getStopType(StopTime stopTime) {
        if (stopTime.getPickupType().equals("0") && stopTime.getDropOffType().equals("1")) {
            return StopType.START;
        } else if (stopTime.getPickupType() == "1" && stopTime.getDropOffType() == "0") {
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

}
