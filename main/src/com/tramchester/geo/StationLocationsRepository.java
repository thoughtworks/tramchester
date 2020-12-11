package com.tramchester.geo;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;

import java.util.List;
import java.util.Set;

@ImplementedBy(StationLocations.class)
public interface StationLocationsRepository {
    LatLong getStationPosition(Station station) throws TransformException;

    HasGridPosition getStationGridPosition(Station station);

    List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, double rangeInKM);

    @NotNull List<Station> nearestStationsSorted(@NotNull HasGridPosition gridPosition, int maxToFind, double rangeInKM);

    Set<Station> nearestStationsUnsorted(Station station, double rangeInKM);

    BoundingBox getBounds();

}
