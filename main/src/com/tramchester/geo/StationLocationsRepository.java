package com.tramchester.geo;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.jetbrains.annotations.NotNull;
import org.opengis.referencing.operation.TransformException;

import java.util.List;
import java.util.stream.Stream;

@ImplementedBy(StationLocations.class)
public interface StationLocationsRepository {

    List<Station> nearestStationsSorted(LatLong latLong, int maxToFind, MarginInMeters rangeInMeters);

    Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters);

    BoundingBox getBounds();

}
