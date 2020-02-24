package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class RouteDTO {

    private List<StationDTO> stations;
    private String routeName;
    private String displayClass;
    private String shortName;

    public RouteDTO(String routeName, String shortName, List<StationDTO> stations, String displayClass) {
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

    public List<StationDTO> getStations() {
        return stations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteDTO routeDTO = (RouteDTO) o;

        return routeName != null ? routeName.equals(routeDTO.routeName) : routeDTO.routeName == null;

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
