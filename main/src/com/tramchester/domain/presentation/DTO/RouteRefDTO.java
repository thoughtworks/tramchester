package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tramchester.domain.Route;
import com.tramchester.domain.TransportMode;

public class RouteRefDTO {

    private String id;
    private String routeName;
    private TransportMode transportMode;
    private String shortName;

    @SuppressWarnings("unused")
    public RouteRefDTO() {
        // deserialization
    }

    public RouteRefDTO(Route route) {
        this.id = route.getId().forDTO();
        this.routeName = route.getName();
        this.transportMode = route.getTransportMode();
        this.shortName = route.getShortName();
    }

    public String getId() {
        return id;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getShortName() {
        return shortName;
    }

    public TransportMode getTransportMode() {
        return transportMode;
    }

    // use TransportMode
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Boolean isTram() {
        return transportMode.equals(TransportMode.Tram);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteRefDTO that = (RouteRefDTO) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
