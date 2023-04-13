package com.tramchester.geo;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

@ImplementedBy(StationLocations.class)
public interface StationLocationsRepository {

    List<Station> nearestStationsSorted(Location<?> location, int maxToFind, MarginInMeters rangeInMeters, EnumSet<TransportMode> modes);

    Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters);

    BoundingBox getActiveStationBounds();

    LocationSet getLocationsWithin(IdFor<NaptanArea> areaId);

    List<LatLong> getBoundaryFor(IdFor<NaptanArea> areaId);

    boolean hasStationsOrPlatformsIn(IdFor<NaptanArea> areaId);

    boolean withinBounds(Location<?> location);
}
