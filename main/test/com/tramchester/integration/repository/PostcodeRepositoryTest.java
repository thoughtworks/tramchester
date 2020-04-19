package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.IntegrationTramTestConfig;
import com.tramchester.repository.PostcodeRepository;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class PostcodeRepositoryTest {

    private static Dependencies dependencies;
    private PostcodeRepository repository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void onceBeforeEachTestRuns() {
        repository = dependencies.get(PostcodeRepository.class);
    }

    @Test
    public void shouldLoadPostcodes() {
        // TODO WIP
        PostcodeLocation result = repository.getPostcode("M139WL");
        double lat = 53.4620378;
        double lon = -2.2280871;

        LatLong position = result.getLatLong();
        assertEquals(lat, position.getLat(), 0.001);
        assertEquals(lon, position.getLon(), 0.001);
    }
}
