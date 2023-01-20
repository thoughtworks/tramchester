package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.AgencyRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.tramchester.integration.testSupport.rail.RailStationIds.*;
import static org.junit.jupiter.api.Assertions.*;

@TrainTest
public class RailRouteIdRepositoryTest {
    private static ComponentContainer componentContainer;
    private RailRouteIdRepository idRepository;
    private IdFor<Agency> agencyId;
    private StationRepository stationRepository;
    private AgencyRepository agencyRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationRailTestConfig config = new IntegrationRailTestConfig();
        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        idRepository = componentContainer.get(RailRouteIdRepository.class);
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
    void shouldHaveLondonToManchesterViaStoke() {

        List<Station> fromManchesterViaWilmslow = getStations(ManchesterPiccadilly, Stockport,
                Wilmslow, Crewe, LondonEuston);

        IdFor<Route> viaWilmslow = idRepository.getRouteIdFor(agencyId, fromManchesterViaWilmslow);

        List<Station> fromManchesterViaMacc = getStations(ManchesterPiccadilly, Stockport,
                Macclesfield, StokeOnTrent, LondonEuston);

        IdFor<Route> viaMacc = idRepository.getRouteIdFor(agencyId, fromManchesterViaMacc);

        assertNotEquals(viaWilmslow, viaMacc);
    }

    private List<Station> getStations(RailStationIds... railStationsIds) {
        return Arrays.stream(railStationsIds).map(id -> id.from(stationRepository)).collect(Collectors.toList());
    }

    @Test
    void shouldHaveLondonToManchesterWhereOneRouteIsSubsetOfOther() {

        List<Station> fromManchester = getStations(ManchesterPiccadilly, Stockport, Wilmslow, Crewe, LondonEuston);

        IdFor<Route> routeIdA = idRepository.getRouteIdFor(agencyId, fromManchester);

        List<Station> fromManchesterWithoutCrewe = getStations(ManchesterPiccadilly, Stockport, Wilmslow, LondonEuston);

        IdFor<Route> routeIdB = idRepository.getRouteIdFor(agencyId, fromManchesterWithoutCrewe);

        assertEquals(routeIdA, routeIdB);
    }

    // WIP
    @Test
    void shouldCheckIdForCallingPointsWhenOneNotMarkedAsInterchange() {
        // need to find 2 "routes" one of which skips a station not marked as interchange
        // but how often does this actually happen?

        // need to keep some details inside of rail route id to make calc possible

        assertFalse(Hale.from(stationRepository).isMarkedInterchange());

//        List<Station> routeA = getStations(Belper, Duffield, Derby);
//        List<Station> routeB = getStations(Belper, Derby);
//
//        IdFor<Route> routeIdA = idRepository.getRouteFor(agencyId, routeA);
//        IdFor<Route> routeIdB = idRepository.getRouteFor(agencyId, belperToDerby);
//
//        assertEquals(routeIdA, routeIdB);
    }

}
