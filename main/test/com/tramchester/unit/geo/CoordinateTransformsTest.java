package com.tramchester.unit.geo;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateTransformsTest {

    @Test
    void shouldConvertToGridCorrectly() {

        double lat = 52.940190;
        double lon = -1.4965572;

        LatLong latLong = new LatLong(lat, lon);

        GridPosition result = CoordinateTransforms.getGridPosition(latLong);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        assertEquals(expectedEasting, result.getEastings());
        assertEquals(expectedNorthing, result.getNorthings());
        assertTrue(result.isValid());
    }

    @Test
    void shouldConvertToLatLongCorrectly() {

        long easting = 433931;
        long northing = 338207;
        GridPosition position = new GridPosition(easting, northing);

        LatLong result = CoordinateTransforms.getLatLong(position);

        double lat = 52.94018971498456;
        double lon = -1.496557148808237;
        assertEquals(lat, result.getLat(), 0.00000000001);
        assertEquals(lon, result.getLon(), 0.00000000001);
        assertTrue(result.isValid());
    }

    @Test
    void shouldConvertInvalidLatLongToInvalid() {
        LatLong  latLong = LatLong.Invalid;
        GridPosition position = CoordinateTransforms.getGridPosition(latLong);
        assertFalse(position.isValid());
    }

    @Test
    void shouldConvertInvalidGridToInvalid() {
       GridPosition gridPosition = GridPosition.Invalid;
       LatLong result = CoordinateTransforms.getLatLong(gridPosition);
       assertFalse(result.isValid());
    }

    @Test
    void shouldConvertForWythenshaweHops() {
        long easting = 380598;
        long northing = 387938;
        GridPosition position = new GridPosition(easting, northing);

        LatLong result = CoordinateTransforms.getLatLong(position);

        LatLong expected = TestEnv.nearWythenshaweHosp;
        assertEquals(expected.getLat(), result.getLat(), 0.01);
        assertEquals(expected.getLon(), result.getLon(), 0.01);
    }

    @Test
    void shouldConvertForRailFormatGrid() {
        LatLong derby = new LatLong(52.9161645,-1.4655347);

        @NotNull GridPosition grid = CoordinateTransforms.getGridPosition(derby);

        assertEquals(436036, grid.getEastings());
        assertEquals(335549, grid.getNorthings());
    }
}
