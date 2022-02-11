package com.tramchester.geo;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.stream.Stream;

@ImplementedBy(StationLocations.class)
public interface StationLocationsRepository {

    List<Station> nearestStationsSorted(Location<?> location, int maxToFind, MarginInMeters rangeInMeters);

    Stream<Station> nearestStationsUnsorted(Station station, MarginInMeters rangeInMeters);

    BoundingBox getBounds();

    LocationSet getLocationsWithin(IdFor<NaptanArea> areaId);

    Geometry getGeometryForArea(IdFor<NaptanArea> areaId);

    List<LatLong> getBoundaryFor(IdFor<NaptanArea> areaId);

    boolean hasStationsOrPlatformsIn(IdFor<NaptanArea> areaId);

    boolean withinBounds(Location<?> location);
}
