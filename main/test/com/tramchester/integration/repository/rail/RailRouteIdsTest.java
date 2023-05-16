package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.RemoteDataAvailable;
import com.tramchester.dataimport.rail.ProvidesRailTimetableRecords;
import com.tramchester.dataimport.rail.RailRouteIDBuilder;
import com.tramchester.dataimport.rail.RailTransportDataFromFiles;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteCallingPoints;
import com.tramchester.dataimport.rail.repository.RailRouteIds;
import com.tramchester.dataimport.rail.repository.RailStationRecordsRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.StationIdPair;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.RailRouteId;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.ProvidesNow;
import com.tramchester.graph.filters.GraphFilterActive;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.metrics.CacheMetrics;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportDataContainer;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.GMTest;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.tramchester.dataimport.rail.reference.TrainOperatingCompanies.TP;
import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static com.tramchester.testSupport.TestEnv.assertSetEquals;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
@GMTest
public class RailRouteIdsTest {
    public static final List<String> LONGEST_VIA_STOKE = Arrays.asList("MNCRPIC", "STKP", "MACLSFD", "STOKEOT", "STAFFRD", "WVRMPTN", "SNDWDUD",
            "BHAMNWS", "BHAMINT", "COVNTRY", "RUGBY", "MKNSCEN", "WATFDJ", "EUSTON");

    private static ComponentContainer componentContainer;
    private static TramchesterConfig config;
    private RailRouteIds railRouteIdRepository;
    private IdFor<Agency> agencyId;
    private StationRepository stationRepository;
    private AgencyRepository agencyRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        railRouteIdRepository = componentContainer.get(RailRouteIds.class);
        stationRepository = componentContainer.get(StationRepository.class);
        agencyRepository = componentContainer.get(AgencyRepository.class);
        agencyId = Agency.createId(TrainOperatingCompanies.VT.name());
    }

    @Test
    void shouldHaveRealAgencyId() {
        assertNotNull(agencyRepository.get(agencyId),
                "did not find " + agencyId + " valid are " + HasId.asIds(agencyRepository.getAgencies()));
    }

    @Test
    void shouldHaveExpecetdIdsForRoutesOfSameLength() {
        List<IdFor<Station>> idsA = asStationIds(Arrays.asList("MNCRIAP", "MNCRPIC", "MNCROXR", "MNCRVIC", "HDRSFLD",
                "DWBY", "LEEDS", "GARFRTH",
                "YORK", "THIRSK", "NLRTN", "YAAM", "TABY", "MDLSBRO", "REDCARC", "SBRN"));

        List<IdFor<Station>> idsB = asStationIds(Arrays.asList("MNCRIAP","GATLEY","MNCRPIC","MNCROXR","MNCRVIC","HDRSFLD",
                "DWBY","LEEDS",
                "YORK","THIRSK","NLRTN","YAAM","TABY","MDLSBRO","REDCARC","SBRN"));

        RailRouteId idA = railRouteIdRepository.getRouteId(new RailRouteCallingPoints(TP.getAgencyId(), idsA));
        RailRouteId idB = railRouteIdRepository.getRouteId(new RailRouteCallingPoints(TP.getAgencyId(), idsB));

        assertNotEquals(idA, idB);
        assertEquals(Route.createId("MNCRIAP:SBRN=>TP:2"), idA);
        assertEquals(Route.createId("MNCRIAP:SBRN=>TP:1"), idB);

    }

    private List<IdFor<Station>> asStationIds(List<String> asList) {
        return asList.stream().map(Station::createId).collect(Collectors.toList());
    }

    @Test
    void shouldHaveDiffRouteIdsForMaccAndWilmslowRoutes() {

        List<Station> fromManchesterViaWilmslow = getStations(ManchesterPiccadilly, Stockport,
                Wilmslow, Crewe, LondonEuston);

        IdFor<Route> viaWilmslow = railRouteIdRepository.getRouteIdUncached(agencyId, fromManchesterViaWilmslow);

        List<Station> fromManchesterViaMacc = getStations(ManchesterPiccadilly, Stockport,
                Macclesfield, StokeOnTrent, LondonEuston);

        IdFor<Route> viaMacc = railRouteIdRepository.getRouteIdUncached(agencyId, fromManchesterViaMacc);

        assertNotEquals(viaWilmslow, viaMacc);
    }

    @Test
    void shouldHaveLondonToManchesterWhereOneRouteIsSubsetOfOther() {

        List<Station> fromManchester = getStations(ManchesterPiccadilly, Stockport, Wilmslow, Crewe, LondonEuston);

        IdFor<Route> routeIdA = railRouteIdRepository.getRouteIdFor(agencyId, fromManchester);

        List<Station> fromManchesterWithoutCrewe = getStations(ManchesterPiccadilly, Stockport, Wilmslow, LondonEuston);

        IdFor<Route> routeIdB = railRouteIdRepository.getRouteIdFor(agencyId, fromManchesterWithoutCrewe);

        assertEquals(routeIdA, routeIdB);
    }

    @Test
    void shouldHaveContainsForAgencyCallingPoints() {

        List<IdFor<Station>> idListA = LONGEST_VIA_STOKE.stream().map(Station::createId).collect(Collectors.toList());

        RailRouteCallingPoints callingA = new RailRouteCallingPoints(agencyId, idListA);

        assertTrue(callingA.contains(callingA));

        List<IdFor<Station>> idListB = getStationIds(ManchesterPiccadilly, Stockport, Crewe, LondonEuston);
        RailRouteCallingPoints callingB = new RailRouteCallingPoints(agencyId, idListB);

        assertFalse(callingA.contains(callingB));
        assertFalse(callingB.contains(callingA));

        List<IdFor<Station>> idListC = getStationIds(ManchesterPiccadilly, Stockport, LondonEuston);
        RailRouteCallingPoints callingC = new RailRouteCallingPoints(agencyId, idListC);

        assertTrue(callingA.contains(callingC));
        assertFalse(callingC.contains(callingA));
        assertFalse(callingC.contains(callingB));

        List<IdFor<Station>> idListD = getStationIds(ManchesterPiccadilly, Stockport, Macclesfield, StokeOnTrent, LondonEuston);
        RailRouteCallingPoints callingD = new RailRouteCallingPoints(agencyId, idListD);

        assertTrue(callingA.contains(callingD));
        assertTrue(callingD.contains(callingC));
        assertFalse(callingD.contains(callingB));

    }

    @Test
    void shouldHaveLongestRouteAndSubsetsWithSameId() {

        StationIdPair beginEnd = StationIdPair.of(ManchesterPiccadilly.getId(), LondonEuston.getId());

        Optional<RailRouteIds.RailRouteCallingPointsWithRouteId> findLongest = railRouteIdRepository.getCallingPointsFor(TrainOperatingCompanies.VT.getAgencyId()).stream().
                filter(railRoute -> railRoute.getBeginEnd().equals(beginEnd)).
                max(Comparator.comparingInt(RailRouteIds.RailRouteCallingPointsWithRouteId::numberCallingPoints));

        assertTrue(findLongest.isPresent());

        RailRouteIds.RailRouteCallingPointsWithRouteId longest = findLongest.get();
        IdFor<Route> routeIdForLongest = longest.getRouteId();

        IdFor<Route> routeIdForShorter = railRouteIdRepository.getRouteIdFor(agencyId, getStations(ManchesterPiccadilly, Stockport, Macclesfield,
                StokeOnTrent, LondonEuston));

        assertEquals(routeIdForLongest, routeIdForShorter);

    }

    @Test
    void shouldHaveExpectedNumberOfIdsForManchesterToLondonEuston() {

        StationIdPair manchesterLondon = StationIdPair.of(ManchesterPiccadilly.getId(), LondonEuston.getId());
        List<RailRouteIds.RailRouteCallingPointsWithRouteId> routes = railRouteIdRepository.getCallingPointsFor(agencyId).stream().
                filter(callingPoints -> callingPoints.getBeginEnd().equals(manchesterLondon)).
                collect(Collectors.toList());

        // was 36 under old ID scheme
        assertEquals(6, routes.size(), routes.toString());
    }

    @Test
    void shouldHaveConsistencyInIndexingOfRoutes() {
        final IdFor<Agency> agencyId = TP.getAgencyId();

        final Set<RailRouteIds.RailRouteCallingPointsWithRouteId> allForTP = railRouteIdRepository.getCallingPointsFor(agencyId);

        final Set<RailRouteIds.RailRouteCallingPointsWithRouteId> allTPCalingPoints = allForTP.stream().
                filter(withIds -> withIds.getBeginEnd().getBeginId().equals(ManchesterAirport.getId())).
                filter(withIds -> withIds.getBeginEnd().getEndId().equals(Saltburn.getId())).
                collect(Collectors.toSet());

        assertFalse(allTPCalingPoints.isEmpty());

        final Map<IdFor<Route>, List<IdFor<Station>>> originalIds = new HashMap<>();
        allTPCalingPoints.forEach(callingPoints -> originalIds.put(callingPoints.getRouteId(), callingPoints.getCallingPoints()));

        final RailRouteIDBuilder railRouteIDBuilder = componentContainer.get(RailRouteIDBuilder.class);

        // go back to original list of calling stations

        final Set<List<IdFor<Station>>> callingStationsForTPs = allTPCalingPoints.stream().
                map(RailRouteIds.RailRouteCallingPointsWithRouteId::getCallingPoints).
                collect(Collectors.toSet());

        final List<RailRouteCallingPoints> railCallingPoints = callingStationsForTPs.stream().
                map(stations -> new RailRouteCallingPoints(agencyId, stations)).
                collect(Collectors.toList());

        // now create id's afresh and check get same callingPoints and routeIds

        for (int i = 0; i < 100000; i++) {
            final Set<RailRouteIds.RailRouteCallingPointsWithRouteId> rawIds = railRouteIDBuilder.getRouteIdsFor(agencyId, railCallingPoints);

            final Map<IdFor<Route>, List<IdFor<Station>>> recreatedIds = new HashMap<>();
            rawIds.forEach(callingPoints -> recreatedIds.put(callingPoints.getRouteId(), callingPoints.getCallingPoints()));

            assertEquals(originalIds, recreatedIds);
        }

    }

    @Test
    void shouldHaveLoadConsistencyCheckWhenOnlyRouteIdRepositoryRestarted() {

        // repro issue where different results when repos is stopped and restarted

        ProvidesRailTimetableRecords loadsRecords = componentContainer.get(ProvidesRailTimetableRecords.class);
        GraphFilterActive graphFilterActive = componentContainer.get(GraphFilterActive.class);
        RemoteDataAvailable remoteDataRefreshed = componentContainer.get(RemoteDataAvailable.class);
        RailStationRecordsRepository stationRecordsRepository = componentContainer.get(RailStationRecordsRepository.class);
        CacheMetrics cacheMetrics = componentContainer.get(CacheMetrics.class);
        final ProvidesNow providesNow = componentContainer.get(ProvidesNow.class);

        RailRouteIDBuilder idBuilder = new RailRouteIDBuilder();

        RailRouteIds routeIdRepository = new RailRouteIds(stationRecordsRepository, loadsRecords, idBuilder, config, cacheMetrics);
        routeIdRepository.start();

        final RailTransportDataFromFiles loadFromFiles = new RailTransportDataFromFiles(loadsRecords,
                config, graphFilterActive, remoteDataRefreshed, routeIdRepository, stationRecordsRepository);

        final Set<RouteStation> first = getRouteStations(providesNow, "first load", loadFromFiles);
        final Set<RouteStation> second = getRouteStations(providesNow, "second load", loadFromFiles);

        assertSetEquals(first, second);

        routeIdRepository.stop();
        routeIdRepository.start();

        final Set<RouteStation> third = getRouteStations(providesNow, "third load", loadFromFiles);

        assertSetEquals(first, third);

    }

    private Set<RouteStation> getRouteStations(ProvidesNow providesNow, String first_load, RailTransportDataFromFiles loadFromFiles) {
        final TransportDataContainer contained = new TransportDataContainer(providesNow, first_load);
        loadFromFiles.loadInto(contained);
        return contained.getRouteStations();
    }

    private List<IdFor<Station>> getStationIds(RailStationIds... railStationIds) {
        return Arrays.stream(railStationIds).map(RailStationIds::getId).collect(Collectors.toList());
    }

    private List<Station> getStations(RailStationIds... railStationsIds) {
        return Arrays.stream(railStationsIds).map(id -> id.from(stationRepository)).collect(Collectors.toList());
    }


}
