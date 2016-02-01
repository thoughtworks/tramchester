package com.tramchester.domain.presentation;


import com.fasterxml.jackson.databind.ObjectMapper;
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
}
