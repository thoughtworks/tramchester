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
}
