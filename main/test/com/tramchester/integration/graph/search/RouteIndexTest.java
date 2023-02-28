package com.tramchester.integration.graph.search;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.caching.LoaderSaverFactory;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.domain.Route;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdSet;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramRouteHelper;
import com.tramchester.testSupport.reference.KnownTramRoute;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class RouteIndexTest {

    private static Path cacheFile;
    private static GuiceContainerDependencies componentContainer;

    private RouteRepository routeRepository;
    private LoaderSaverFactory factory;
    private RouteIndex routeIndex;
    private TramRouteHelper routeHelper;
    private TramDate date;

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {
        final Path cacheFolder = tramchesterConfig.getCacheFolder();

        cacheFile = cacheFolder.resolve(RouteToRouteCosts.INDEX_FILE);

        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {

        // Clear Cache
        TestEnv.clearDataCache(componentContainer); // => this removes the index cache
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        routeRepository = componentContainer.get(RouteRepository.class);
        factory = componentContainer.get(LoaderSaverFactory.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        routeHelper = new TramRouteHelper(routeRepository);
        date = TestEnv.testDay();
    }

    @Test
    void shouldHaveMatchedContentInCacheFile() {

        assertTrue(cacheFile.toFile().exists(), "Missing " + cacheFile.toAbsolutePath());

        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, cacheFile);

        Stream<RouteIndexData> indexFromFile = loader.load();

        macthesRouteRepository(indexFromFile);
    }

    @Test
    void shouldHaveIndexForAllKnownRoutes() {

        for (int i = 0; i < KnownTramRoute.values().length; i++) {
            Route route = routeHelper.getOneRoute(KnownTramRoute.RochdaleShawandCromptonManchesterEastDidisbury, date);
            int index = routeIndex.indexFor(route.getId()); // throws on error

            Route result = routeIndex.getRouteFor(index);
            assertEquals(route.getId(), result.getId());
        }
    }

    @Test
    void shouldSaveToCacheAndReload() throws IOException {

        Path otherFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".fortesting.json");
        Files.deleteIfExists(otherFile);

        routeIndex.cacheTo(factory.getDataSaverFor(RouteIndexData.class, otherFile));

        assertTrue(otherFile.toFile().exists(), "Missing " + otherFile.toAbsolutePath());

        assertTrue(Files.exists(otherFile));

        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, otherFile);

        Stream<RouteIndexData> indexFromFile = loader.load();

        macthesRouteRepository(indexFromFile);
    }

    private void macthesRouteRepository(Stream<RouteIndexData> loaded) {
        List<RouteIndexData> resultsForIndex = loaded.collect(Collectors.toList());

        IdSet<Route> expected = routeRepository.getRoutes().stream().collect(IdSet.collector());

        assertEquals(expected.size(), resultsForIndex.size());

        IdSet<Route> idsFromIndex = resultsForIndex.stream().map(RouteIndexData::getRouteId).collect(IdSet.idCollector());
        assertEquals(expected, idsFromIndex);
    }


}
