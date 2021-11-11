package com.tramchester.domain.presentation.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.RouteReadOnly;
import com.tramchester.domain.reference.TransportMode;

@JsonIgnoreProperties(value = "tram", allowGetters = true)
public class RouteRefDTO {

    private String routeName;
    private TransportMode transportMode;
    private String shortName;

    @SuppressWarnings("unused")
    public RouteRefDTO() {
        // deserialization
    }

    public RouteRefDTO(RouteReadOnly route) {
        this(route, route.getName());
    }

    public RouteRefDTO(RouteReadOnly route, String routeName) {
        this.routeName = routeName;

        // tfgm data have routes that are identical except for the ID, don't want to expose this to the API
        //this.id = route.getId().forDTO();

        this.transportMode = route.getTransportMode();
        this.shortName = route.getShortName();
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
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public Boolean isTram() {
        return transportMode.equals(TransportMode.Tram);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteRefDTO that = (RouteRefDTO) o;

        return routeName.equals(that.routeName);
    }

    @Override
    public int hashCode() {
        return routeName.hashCode();
    }

    @Override
    public String toString() {
        return "RouteRefDTO{" +
                ", routeName='" + routeName + '\'' +
                ", transportMode=" + transportMode +
                ", shortName='" + shortName + '\'' +
                '}';
    }
}
