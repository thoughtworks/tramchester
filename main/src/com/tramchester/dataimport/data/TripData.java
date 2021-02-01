package com.tramchester.dataimport.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Route;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;

public class TripData {

    @JsonProperty("route_id")
    private String routeId;
    @JsonProperty("service_id")
    private String serviceId;
    @JsonProperty("trip_id")
    private String tripId;
    private String headsign;

    public TripData() {
        // deserialization
    }

    @JsonProperty("trip_headsign")
    private void setHeadsign(String text) {
        if (text.contains(Station.TRAM_STATION_POSTFIX)) {
            setForMetrolink(text);
        } else {
            headsign = text;
        }
    }

    private void setForMetrolink(String text) {
        text = text.replace(Station.TRAM_STATION_POSTFIX,"").trim();
        int indexOfDivider = text.indexOf(",");
        if (indexOfDivider>0) {
            // just the station part if present
            headsign = text.substring(indexOfDivider+1).trim();
        } else {
            headsign = text;
        }
    }

    private String removeSpaces(String text) {
        return text.replaceAll(" ","");
    }

    public IdFor<Route> getRouteId() {
        return IdFor.createId(removeSpaces(routeId));
    }

    public IdFor<Service> getServiceId() {
        return  IdFor.createId(serviceId);
    }

    public IdFor<Trip> getTripId() {
        return IdFor.createId(tripId);
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
