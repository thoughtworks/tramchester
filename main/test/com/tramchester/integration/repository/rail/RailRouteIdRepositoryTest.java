package com.tramchester.integration.repository.rail;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.dataimport.rail.reference.TrainOperatingCompanies;
import com.tramchester.dataimport.rail.repository.RailRouteIdRepository;
import com.tramchester.domain.Agency;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.rail.IntegrationRailTestConfig;
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
        agencyId = Agency.createId(TrainOperatingCompanies.VT.name());


    }

    @Test
    void shouldHaveLondonToManchesterViaStoke() {

        List<IdFor<Station>> fromManchesterViaWilmslow = Arrays.asList(ManchesterPiccadilly.getId(), Stockport.getId(),
                Wilmslow.getId(), Crewe.getId(), LondonEuston.getId());

        IdFor<Route> viaWilmslow = idRepository.getRouteIdForCallingPointsAndAgency(agencyId, fromManchesterViaWilmslow);

        List<IdFor<Station>> fromManchesterViaMacc = Arrays.asList(ManchesterPiccadilly.getId(), Stockport.getId(),
                Macclesfield.getId(), StokeOnTrent.getId(), LondonEuston.getId());

        IdFor<Route> viaMacc = idRepository.getRouteIdForCallingPointsAndAgency(agencyId, fromManchesterViaMacc);

        assertNotEquals(viaWilmslow, viaMacc);
    }

    @Test
    void shouldHaveLondonToManchesterWhereOneRouteIsSubsetOfOther() {

        List<IdFor<Station>> fromManchester = Arrays.asList(ManchesterPiccadilly.getId(), Stockport.getId(),
                Wilmslow.getId(), Crewe.getId(), LondonEuston.getId());

        IdFor<Route> routeIdA = idRepository.getRouteIdForCallingPointsAndAgency(agencyId, fromManchester);

        List<IdFor<Station>> fromManchesterWithoutCrewe = Arrays.asList(ManchesterPiccadilly.getId(), Stockport.getId(),
                Wilmslow.getId(), LondonEuston.getId());

        IdFor<Route> routeIdB = idRepository.getRouteIdForCallingPointsAndAgency(agencyId, fromManchesterWithoutCrewe);

        assertEquals(routeIdA, routeIdB);
    }

}
