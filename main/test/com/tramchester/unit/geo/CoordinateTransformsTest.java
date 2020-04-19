package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.StationLocations;
import org.junit.Test;
import org.opengis.referencing.operation.TransformException;

import static org.junit.Assert.assertEquals;

public class CoordinateTransformsTest {

    @Test
    public void shouldConvertToGridCorrectly() throws TransformException {
        CoordinateTransforms transforms = new CoordinateTransforms();

        double lat = 52.940190;
        double lon = -1.4965572;

        LatLong latLong = new LatLong(lat, lon);

        StationLocations.GridPosition result = transforms.getGridPosition(latLong);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        assertEquals(expectedEasting, result.getEastings());
        assertEquals(expectedNorthing, result.getNorthings());
    }

    @Test
    public void shouldConvertToLatLongCorrectly() throws TransformException {
        CoordinateTransforms transforms = new CoordinateTransforms();

        long easting = 433931;
        long northing = 338207;

        LatLong result = transforms.getLatLong(easting, northing);

        double lat = 52.94018971498456;
        double lon = -1.496557148808237;
        assertEquals(lat, result.getLat(), 0.00000000001);
        assertEquals(lon, result.getLon(), 0.00000000001);
    }
}
