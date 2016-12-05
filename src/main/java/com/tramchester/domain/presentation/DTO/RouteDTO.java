package com.tramchester.domain.presentation.DTO;

import java.util.List;

public class RouteDTO {

    private List<StationDTO> stations;

    public RouteDTO() {
        // deserialisation
    }

    private String routeName;

    public RouteDTO(String routeName, List<StationDTO> stations) {
        this.stations = stations;
        this.routeName = routeName;
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
}
