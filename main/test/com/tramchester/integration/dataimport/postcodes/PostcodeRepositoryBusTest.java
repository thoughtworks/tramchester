package com.tramchester.integration.dataimport.postcodes;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.dataimport.postcodes.PostcodeData;
import com.tramchester.dataimport.postcodes.PostcodeDataImporter;
import com.tramchester.domain.id.CaseInsensitiveId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.MarginInMeters;
import com.tramchester.geo.StationLocations;
import com.tramchester.integration.testSupport.bus.IntegrationBusTestConfig;
import com.tramchester.integration.testSupport.tram.TramWithPostcodesEnabled;
import com.tramchester.repository.postcodes.PostcodeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.BusTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@BusTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class PostcodeRepositoryBusTest {

    private static ComponentContainer componentContainer;
    private PostcodeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig testConfig = new IntegrationBusTestConfig();
        componentContainer = new ComponentsBuilder().create(testConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = componentContainer.get(PostcodeRepository.class);
    }
    
    @Test
    void shouldLoadLocalPostcodesFromFilesInLocation() {

        assertFalse(repository.hasPostcode(CaseInsensitiveId.createIdFor("EC1A1XH"))); // in london, outside area
        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("WA141EP")));
        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("wa141ep")));

        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("M44BF"))); // central manchester
        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor(TestEnv.postcodeForWythenshaweHosp())));

        assertTrue(repository.hasPostcode(CaseInsensitiveId.createIdFor("WA160BE")));

    }


}
