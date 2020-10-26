package com.tramchester.integration.repository;

import com.tramchester.Dependencies;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.TramWithPostcodesEnabled;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PostcodeRepositoryTest {

    private static Dependencies dependencies;
    private PostcodeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        dependencies = new Dependencies();
        dependencies.initialise(new TramWithPostcodesEnabled());
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

        LatLong expected = TestEnv.nearWythenshaweHosp();

        PostcodeLocation result = repository.getPostcode(IdFor.createId(TestEnv.postcodeForWythenshaweHosp()));

        assertNotNull(result);
        LatLong position = result.getLatLong();
        Assertions.assertEquals(expected.getLat(), position.getLat(), 0.01);
        Assertions.assertEquals(expected.getLon(), position.getLon(), 0.01);
    }
}
