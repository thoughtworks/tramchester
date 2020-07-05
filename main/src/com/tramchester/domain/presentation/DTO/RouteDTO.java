package com.tramchester.domain.presentation.DTO;

import java.util.List;
import java.util.Objects;

public class RouteDTO {

    private List<StationRefWithPosition> stations;
    private String routeName;
    private String displayClass;
    private String shortName;

    public RouteDTO(String routeName, String shortName, List<StationRefWithPosition> stations, String displayClass) {
        this.shortName = shortName;
        this.stations = stations;
        this.routeName = routeName;
        this.displayClass = displayClass;
    }

    public RouteDTO() {
        // deserialisation
    }

    public String getRouteName() {
        return routeName;
    }

    public List<StationRefWithPosition> getStations() {
        return stations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteDTO routeDTO = (RouteDTO) o;

        return Objects.equals(routeName, routeDTO.routeName);

    }

    @Override
    public int hashCode() {
        return routeName != null ? routeName.hashCode() : 0;
    }

    public String getDisplayClass() {
        return displayClass;
    }

    public String getShortName() {
        return shortName;
    }
}
