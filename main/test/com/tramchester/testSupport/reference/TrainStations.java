package com.tramchester.testSupport.reference;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestStation;
import com.tramchester.testSupport.TestStations;
import org.jetbrains.annotations.NotNull;

public enum TrainStations implements TestStations {
    ManchesterPiccadilly("MAN", "Manchester Piccadilly", new LatLong(53.47736,-2.23091)),
    LondonEuston("EUS", "London Euston", new LatLong(51.52814,-0.13392));

    private final TestStation station;

    TrainStations(String id, String name, LatLong latlong) {
        @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(latlong);
        this.station = new TestStation(id, "", name, latlong, grid, TransportMode.Train);
    }

    public static Station of(TrainStations enumValue) {
        return enumValue.station;
    }

    public static Station real(StationRepository stationRepository, BusStations station) {
        return stationRepository.getStationById(station.getId());
    }

    @Override
    public IdFor<Station> getId() {
        return station.getId();
    }

    @Override
    public GraphPropertyKey getProp() {
        return station.getProp();
    }

    public String forDTO() {
        return getId().forDTO();
    }
}
