package com.tramchester.unit.domain.presentation;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.presentation.LatLong;
import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class LatLongTest {

    @Test
    public void shouldBeAbleToSerialiseAndDeSerialise() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        LatLong latLong = new LatLong(-1,2);

        String output = mapper.writeValueAsString(latLong);
        assertEquals("{\"lat\":-1.0,\"lon\":2.0}", output);

        LatLong result =mapper.readValue(output, LatLong.class);

        assertEquals(-1, result.getLat(),0);
        assertEquals(2, result.getLon(),0);
    }

    @Test
    public void shouldBeAbleToConvertToACoordinate() {
        LatLong latLong = new LatLong(2, 5);
        Coordinate coordinate = LatLong.getCoordinate(latLong);

        assertEquals(5, coordinate.x, 0);
        assertEquals(2, coordinate.y, 0);
    }


    @Test
    public void shouldBeAbleToSetGet() {
        LatLong latLong = new LatLong();
        latLong.setLat(5);
        latLong.setLon(2);

        assertEquals(5, latLong.getLat(), 0);
        assertEquals(2, latLong.getLon(), 0);
    }
}
