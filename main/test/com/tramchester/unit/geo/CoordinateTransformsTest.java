package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.HasGridPosition;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;

class CoordinateTransformsTest {

    @Test
    void shouldConvertToGridCorrectly() throws TransformException {

        double lat = 52.940190;
        double lon = -1.4965572;

        LatLong latLong = new LatLong(lat, lon);

        HasGridPosition result = CoordinateTransforms.getGridPosition(latLong);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        Assertions.assertEquals(expectedEasting, result.getEastings());
        Assertions.assertEquals(expectedNorthing, result.getNorthings());
    }

    @Test
    void shouldConvertToLatLongCorrectly() throws TransformException {

        long easting = 433931;
        long northing = 338207;

        LatLong result = CoordinateTransforms.getLatLong(easting, northing);

        double lat = 52.94018971498456;
        double lon = -1.496557148808237;
        Assertions.assertEquals(lat, result.getLat(), 0.00000000001);
        Assertions.assertEquals(lon, result.getLon(), 0.00000000001);
    }

    @Test
    void shouldConvertForWythenshaweHops() throws TransformException {
        long easting = 380598;
        long northing = 387938;

        LatLong result = CoordinateTransforms.getLatLong(easting, northing);

        LatLong expected = TestEnv.nearWythenshaweHosp();
        Assertions.assertEquals(expected.getLat(), result.getLat(), 0.01);
        Assertions.assertEquals(expected.getLon(), result.getLon(), 0.01);
    }
}
