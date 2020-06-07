package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.PostcodeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

class PostcodeRepositoryTest {

    private static Dependencies dependencies;
    private PostcodeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
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
        PostcodeLocation result = repository.getPostcode("M139WL");
        double lat = 53.4620378;
        double lon = -2.2280871;

        LatLong position = result.getLatLong();
        Assertions.assertEquals(lat, position.getLat(), 0.001);
        Assertions.assertEquals(lon, position.getLon(), 0.001);
    }
}
