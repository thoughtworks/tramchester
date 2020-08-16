package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.testSupport.WithPostcodesEnabled;
import org.junit.jupiter.api.*;

import java.io.IOException;

class PostcodeRepositoryTest {

    private static Dependencies dependencies;
    private PostcodeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new WithPostcodesEnabled());
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        repository = dependencies.get(PostcodeRepository.class);
    }

    @Test
    void shouldLoadPostcodes() {
        PostcodeLocation result = repository.getPostcode(IdFor.createId("M139WL"));
        double lat = 53.4620378;
        double lon = -2.2280871;

        LatLong position = result.getLatLong();
        Assertions.assertEquals(lat, position.getLat(), 0.001);
        Assertions.assertEquals(lon, position.getLon(), 0.001);
    }
}
