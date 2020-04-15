package com.tramchester.unit.domain.presentation;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.presentation.LatLong;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

//import com.vividsolutions.jts.geom.*;

public class LatLongTest {

    private static final double DELTA = 0.05D;
    private static CoordinateReferenceSystem nationalGridRefSys;
    private static CoordinateReferenceSystem latLongRef;

    @BeforeClass
    public static void onceBeforeAllTests() throws FactoryException {
        CRSAuthorityFactory authorityFactory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG", null);

        nationalGridRefSys = authorityFactory.createCoordinateReferenceSystem("27700");
        latLongRef = authorityFactory.createCoordinateReferenceSystem("4326");

    }

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
    public void shouldBeAbleToSetGet() {
        LatLong latLong = new LatLong();
        latLong.setLat(5);
        latLong.setLon(2);

        assertEquals(5, latLong.getLat(), 0);
        assertEquals(2, latLong.getLon(), 0);
    }

    @Test
    public void shouldConvertEastingsNorthingToLatLong() throws FactoryException, TransformException {
        int easting = 433931;
        int northing = 338207;

        DirectPosition eastNorth = new GeneralDirectPosition(easting, northing);

        CoordinateOperation operation = new DefaultCoordinateOperationFactory().createOperation(nationalGridRefSys, latLongRef);
        DirectPosition latLong = operation.getMathTransform().transform(eastNorth, null);

        double expectedLat = 52.940190;
        double expectedLon = -1.4965572;

        assertEquals(expectedLat, latLong.getOrdinate(0), DELTA);
        assertEquals(expectedLon, latLong.getOrdinate(1), DELTA);

    }

    @Test
    public void shouldConvertLatLongToEastingsNorthing() throws FactoryException, TransformException {
        double lat = 52.940190;
        double lon = -1.4965572;

        DirectPosition latLong = new GeneralDirectPosition(lat, lon);

        CoordinateOperation operation = new DefaultCoordinateOperationFactory().createOperation(latLongRef, nationalGridRefSys);
        DirectPosition nationalGrid = operation.getMathTransform().transform(latLong, null);

        long expectedEasting = 433931;
        long expectedNorthing = 338207;

        long easting = Math.round(nationalGrid.getOrdinate(0));
        long northing = Math.round(nationalGrid.getOrdinate(1));

        assertEquals(expectedEasting, easting);
        assertEquals(expectedNorthing, northing);
    }

}
