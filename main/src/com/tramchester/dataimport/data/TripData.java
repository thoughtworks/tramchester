package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.input.Trip;

@SuppressWarnings("unused")
public class TripData {

    @JsonProperty("route_id")
    private String routeId;
    @JsonProperty("service_id")
    private String serviceId;
    @JsonProperty("trip_id")
    private String tripId;
    @JsonProperty("trip_headsign")
    private String headsign;

    public TripData() {
        // deserialization
    }

    private String removeSpaces(String text) {
        return text.replaceAll(" ","");
    }

    public IdFor<Route> getRouteId() {
        return StringIdFor.createId(removeSpaces(routeId));
    }

    public IdFor<Service> getServiceId() {
        return  StringIdFor.createId(serviceId);
    }

    public IdFor<Trip> getTripId() {
        return StringIdFor.createId(tripId);
    }

    public String getHeadsign() {
        return headsign;
    }

    @Override
    public String toString() {
        return "TripData{" +
                "routeId='" + routeId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", tripId='" + tripId + '\'' +
                ", tripHeadsign='" + headsign + '\'' +
                '}';
    }
}
