package com.tramchester.domain.presentation.DTO;

import com.tramchester.domain.Route;

import java.util.List;

public class RouteDTO extends RouteRefDTO {

    private List<StationRefWithPosition> stations;

    public RouteDTO(Route route, List<StationRefWithPosition> stations) {
        super(route);
        this.stations = stations;
    }

    @SuppressWarnings("unused")
    public RouteDTO() {
        // deserialisation
    }

    public List<StationRefWithPosition> getStations() {
        return stations;
    }

    @Override
    public String toString() {
        return "RouteDTO{" +
                "stations=" + stations +
                "} " + super.toString();
    }
}
