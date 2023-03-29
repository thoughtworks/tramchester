package com.tramchester.integration.graph.search;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.caching.LoaderSaverFactory;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataexport.DataSaver;
import com.tramchester.dataimport.data.RouteIndexData;
import com.tramchester.dataimport.loader.files.TransportDataFromFile;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.*;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.search.routes.RouteIndex;
import com.tramchester.graph.search.routes.RouteToRouteCosts;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.repository.RouteRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import org.checkerframework.checker.units.qual.A;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tramchester.domain.reference.TransportMode.Train;
import static com.tramchester.domain.reference.TransportMode.TramsOnly;
import static org.junit.jupiter.api.Assertions.*;

@GMTest
public class RailAndTramRouteIndexTest extends EasyMockSupport {

    private static Path cacheFile;
    private static GuiceContainerDependencies componentContainer;
    private static Path otherFile;

    private RouteRepository routeRepository;
    private LoaderSaverFactory factory;
    private RouteIndex routeIndex;

    @BeforeAll
    static void onceBeforeAnyTestRuns() throws IOException {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();

        final Path cacheFolder = config.getCacheFolder();

        cacheFile = cacheFolder.resolve(RouteToRouteCosts.INDEX_FILE);
        otherFile = cacheFile.resolveSibling(cacheFile.getFileName() + ".fortesting.json");

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        Files.deleteIfExists(otherFile);

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() throws IOException {
        routeRepository = componentContainer.get(RouteRepository.class);
        factory = componentContainer.get(LoaderSaverFactory.class);
        routeIndex = componentContainer.get(RouteIndex.class);

        Files.deleteIfExists(otherFile);
    }

    @AfterEach
    void onceAfterEachTestRuns() throws IOException {
        Files.deleteIfExists(otherFile);
    }

    @Test
    void shouldHaveMatchedContentInCacheFile() {

        assertTrue(cacheFile.toFile().exists(), "Missing " + cacheFile.toAbsolutePath());

        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, cacheFile);

        Stream<RouteIndexData> indexFromFile = loader.load();

        macthesRouteRepository(indexFromFile);
    }

    @Test
    void shouldGetExpectedRouteIdsBetweenLiverpoolAndScabbrough() {
        // here to double-check if missing route is due to cache or route id creation inconsistencies
        IdFor<Agency> agency = StringIdFor.createId("TP", Agency.class);
        IdFor<Station> first = StringIdFor.createId("LVRPLSH", Station.class);
        IdFor<Station> second = StringIdFor.createId("SCARBRO", Station.class);

        RailRouteIdRepository idRepositoy = componentContainer.get(RailRouteIdRepository.class);
        Set<RailRouteId> forAgency = idRepositoy.getForAgency(agency);


        Set<RailRouteId> forRoute = forAgency.stream().
                filter(railRouteId -> railRouteId.getBegin().equals(first)).
                filter(railRouteId -> railRouteId.getEnd().equals(second)).
                collect(Collectors.toSet());

        assertEquals(3, forRoute.size());

        assertTrue(forRoute.contains(new RailRouteId(first, second, agency, 1)), forRoute.toString());
        assertTrue(forRoute.contains(new RailRouteId(first, second, agency, 2)), forRoute.toString());
        assertTrue(forRoute.contains(new RailRouteId(first, second, agency, 3)), forRoute.toString());

    }

    @Test
    void shouldReproIssueWithMissingRailRouteStationId() {
        // RailRouteId{begin=Id{'Station:LVRPLSH'}, end=Id{'Station:SCARBRO'}, agencyId=Id{'Agency:TP'}, index=1}
        // ContainsId{containedId=Id{'Route:LVRPLSH:SCARBRO=>TP:1'}}

        IdFor<Station> first = StringIdFor.createId("LVRPLSH", Station.class);
        IdFor<Station> second = StringIdFor.createId("SCARBRO", Station.class);
        IdFor<Agency> agency = StringIdFor.createId("TP", Agency.class);

        final Set<Route> trainRoutes = routeRepository.getRoutes(EnumSet.of(Train));

        Set<RailRouteId> matching = trainRoutes.stream().map(route -> (RailRouteId) route.getId()).
                filter(railRouteId -> railRouteId.getAgencyId().equals(agency)).
                filter(railRouteId -> railRouteId.getBegin().equals(first)).
                filter(railRouteId -> railRouteId.getEnd().equals(second)).
                collect(Collectors.toSet());

        assertFalse(matching.isEmpty());

        RailRouteId railRouteId = new RailRouteId(first, second, agency, 1);

        assertTrue(matching.contains(railRouteId), matching.toString());

        assertNotNull(routeRepository.getRouteById(railRouteId));
    }

    @Test
    void shouldRoundTripForTramRouteIds() {

        final Set<Route> tramRoutes = routeRepository.getRoutes(TramsOnly);

        tramRoutes.forEach(tramRoute -> {
            int index = routeIndex.indexFor(tramRoute.getId()); // throws on error

            Route result = routeIndex.getRouteFor(index);
            assertEquals(tramRoute.getId(), result.getId());
        });
    }

    @Test
    void shouldHaveSameSizeInRepositoryAndRouteIndex() {
        long inRepository = new HashSet<>(routeRepository.getRoutes(EnumSet.of(Train))).size();

        long inIndex = routeIndex.sizeFor(Train);

        assertEquals(inRepository, inIndex);
    }

    @Test
    void shouldHaveSameRoutesInRepositoryAndRouteIndex() {
        final Set<Route> trainRoutes = new HashSet<>(routeRepository.getRoutes(EnumSet.of(Train)));

        final Set<Route> missingFromRouteIndex = trainRoutes.stream().
                filter(route -> !routeIndex.hasIndexFor(route.getId())).collect(Collectors.toSet());

        assertTrue(missingFromRouteIndex.isEmpty(), "count " + missingFromRouteIndex.size()
                + "  " + HasId.asIds(missingFromRouteIndex));
    }

    @Test
    void shouldRoundTripForRailRoutes() {
        Set<Route> trainRoutes = new HashSet<>(routeRepository.getRoutes(EnumSet.of(Train)));

        trainRoutes.forEach(trainRoute -> {
            int index = routeIndex.indexFor(trainRoute.getId());
            Route result = routeIndex.getRouteFor(index);
            assertEquals(trainRoute.getId(), result.getId());
        });
    }

    @Test
    void shouldSaveToCacheAndReload() {

        DataSaver<RouteIndexData> saver = factory.getDataSaverFor(RouteIndexData.class, otherFile);
        TransportDataFromFile<RouteIndexData> loader = factory.getDataLoaderFor(RouteIndexData.class, otherFile);

        routeIndex.cacheTo(saver);
        assertTrue(Files.exists(otherFile), "Missing " + otherFile.toAbsolutePath());

        Stream<RouteIndexData> loadedFromFile = loader.load();
        macthesRouteRepository(loadedFromFile);
    }

    private void macthesRouteRepository(Stream<RouteIndexData> loadedFromFile) {
        Set<RouteIndexData> loaded = loadedFromFile.collect(Collectors.toSet());
        IdSet<Route> loadedIds = loaded.stream().map(RouteIndexData::getRouteId).collect(IdSet.idCollector());

        IdSet<Route> expectedIds = routeRepository.getRoutes().stream().collect(IdSet.collector());

        assertEquals(expectedIds.size(), loaded.size());

        // ought to be same but tracking down ID equals/hashCode bug....
        assertEquals(expectedIds.size(), loadedIds.size());

        IdSet<Route> differences = IdSet.disjunction(expectedIds, loadedIds);

        differences.forEach(diff -> assertNotNull(routeRepository.getRouteById(diff), diff.toString()));

        assertTrue(differences.isEmpty(), "count " + differences.size() + " " + differences);
    }


}
