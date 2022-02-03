package com.tramchester.integration.geo;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.LocationSet;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.NaptanArea;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.BusStations;
import com.tramchester.testSupport.reference.TestPostcodes;
import com.tramchester.testSupport.reference.TramStations;
import com.tramchester.testSupport.testTags.BusTest;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.metadata.citation.Citation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
class BusStationsLocationsTest {
    public static final String SHUDEHILL_INTERCHANGE_AREA_CODE = "180GSHIC";
    private static ComponentContainer componentContainer;
    private static IntegrationBusTestConfig testConfig;

    private StationLocations stationLocations;
    private MarginInMeters inMeters;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        stationLocations = componentContainer.get(StationLocations.class);
        inMeters =  MarginInMeters.of(testConfig.getNearestStopForWalkingRangeKM());
    }

    @AfterAll
    static void afterEachTestRuns() {
        componentContainer.close();
    }

    //
    // these tests here to support tuning config parameters for num and distance of stations
    //

    @Test
    void shouldGetAllStationsCloseToPiccGardens() {
        List<Station> result = stationLocations.nearestStationsSorted(TestPostcodes.NearPiccadillyGardens.getLatLong(),
                500, inMeters);
        assertEquals(51, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralBury() {
        List<Station> result = stationLocations.nearestStationsSorted(TestPostcodes.CentralBury.getLatLong(),
                500, inMeters);
        assertEquals(38, result.size());
    }

    @Test
    void shouldGetAllStationsCloseToCentralAlty() {
        List<Station> result = stationLocations.nearestStationsSorted(TramStations.Altrincham.getLatLong(),
                500, inMeters);
        assertEquals(17, result.size());
    }

    @Test
    void shouldGetStationsWithinAnArea() {
        IdFor<NaptanArea> areaId = StringIdFor.createId(SHUDEHILL_INTERCHANGE_AREA_CODE);

        LocationSet result = stationLocations.getLocationsWithin(areaId);

        assertEquals(9, result.size());

        Geometry geometry = stationLocations.getGeometryForArea(areaId);

        Citation model = new CitationImpl("EPSG");
        int srid = Integer.parseInt(DefaultGeographicCRS.WGS84.getIdentifier(model).getCode());

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), srid);
        Point point = geometryFactory.createPoint(BusStations.StopAtShudehillInterchange.getLatLong().getCoordinate());

        assertTrue(geometry.contains(point));
    }

    @Test
    void shouldGetBoundary() {
        IdFor<NaptanArea> areaId = StringIdFor.createId(SHUDEHILL_INTERCHANGE_AREA_CODE);

        List<LatLong> points = stationLocations.getBoundaryFor(areaId);

        assertEquals(10, points.size());
    }
}
