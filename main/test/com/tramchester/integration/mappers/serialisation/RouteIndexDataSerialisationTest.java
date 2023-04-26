package com.tramchester.integration.mappers.serialisation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.caching.LoaderSaverFactory;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.testSupport.TestEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.integration.testSupport.rail.RailStationIds.LondonEuston;
import static com.tramchester.integration.testSupport.rail.RailStationIds.StokeOnTrent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteIndexDataSerialisationTest {

    private ObjectMapper mapper;
    private Path pathToJsonFile;
    private LoaderSaverFactory factory;

    @BeforeEach
    void onceBeforeEachTest() throws IOException {
        pathToJsonFile = TestEnv.getTempDir().resolve("testfile.json");
        Files.deleteIfExists(pathToJsonFile);
        mapper = new ObjectMapper();
        factory = new LoaderSaverFactory();
        factory.start();
    }

    @Test
    void shouldSaveAndLoadToFileRouteId() {

        IdFor<Route> routeId = Route.createId("routeB");
        RouteIndexData routeIndexData = new RouteIndexData((short) 42, routeId);

        saveToFile(routeIndexData);

        assertTrue(Files.exists(pathToJsonFile));

        List<RouteIndexData> loadedData = loadFromFile();

        assertEquals(1, loadedData.size());

        RouteIndexData loadedRouteIndexData = loadedData.get(0);

        assertEquals(routeId, loadedRouteIndexData.getRouteId());
        assertEquals(42, loadedRouteIndexData.getIndex());

    }

    @Test
    void shouldSaveAndLoadToFileRailRouteId() {

        RailRouteId railRouteId = getRailRouteId();

        RouteIndexData routeIndexData = new RouteIndexData((short) 42, railRouteId);

        saveToFile(routeIndexData);

        assertTrue(Files.exists(pathToJsonFile));

        List<RouteIndexData> loadedData = loadFromFile();

        assertEquals(1, loadedData.size());

        RouteIndexData loadedRouteIndexData = loadedData.get(0);

        assertEquals(railRouteId, loadedRouteIndexData.getRouteId());
        assertEquals(42, loadedRouteIndexData.getIndex());

    }

    @Test
    void shouldRoundTripRouteId() throws JsonProcessingException {
        IdFor<Route> routeId = Route.createId("routeB");

        RouteIndexData routeIndexData = new RouteIndexData((short) 42, routeId);

        String asString = mapper.writeValueAsString(routeIndexData);

        RouteIndexData result = mapper.readValue(asString, RouteIndexData.class);

        assertEquals(42, result.getIndex());
        assertEquals(routeId, result.getRouteId());
    }

    @Test
    void shouldRoundTripWithRailRouteId() throws JsonProcessingException {
        RailRouteId railRouteId = getRailRouteId();

        RouteIndexData routeIndexData = new RouteIndexData((short) 56, railRouteId);

        String asString = mapper.writeValueAsString(routeIndexData);

        RouteIndexData result = mapper.readValue(asString, RouteIndexData.class);

        assertEquals(56, result.getIndex());
        assertEquals(railRouteId, result.getRouteId());
    }

    @NotNull
    private RailRouteId getRailRouteId() {
        IdFor<Agency> agencyId = Agency.createId("NT");
        return new RailRouteId(LondonEuston.getId(), StokeOnTrent.getId(), agencyId, 1);
    }

    @NotNull
    private List<RouteIndexData> loadFromFile() {
        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, pathToJsonFile);

        Stream<RouteIndexData> stream = loader.load();

        return stream.collect(Collectors.toList());
    }

    private void saveToFile(RouteIndexData routeIndexData) {
        DataSaver<RouteIndexData> saver = factory.getDataSaverFor(RouteIndexData.class, pathToJsonFile);

        saver.open();
        saver.write(routeIndexData);
        saver.close();
    }

}
