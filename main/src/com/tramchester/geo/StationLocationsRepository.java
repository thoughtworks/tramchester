package com.tramchester.geo;

import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;

import java.util.List;
import java.util.Set;

public interface StationLocationsRepository {
    LatLong getStationPosition(Station station) throws TransformException;

    StationLocations.GridPosition getStationGridPosition(Station station);

    List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, double rangeInKM);

    @NotNull List<Station> nearestStationsSorted(@NotNull StationLocations.GridPosition gridPosition, int maxToFind, double rangeInKM);

    List<Station> nearestStationsUnsorted(Station station, double rangeInKM);

    long getEastingsMax();

    long getEastingsMin();

    long getNorthingsMax();

    long getNorthingsMin();

}
