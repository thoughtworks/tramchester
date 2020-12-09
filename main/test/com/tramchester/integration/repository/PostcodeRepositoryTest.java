package com.tramchester.integration.repository;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.PostcodeLocation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.repository.PostcodeRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.integration.testSupport.TramWithPostcodesEnabled;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PostcodeRepositoryTest {

    private static ComponentContainer componentContainer;
    private PostcodeRepository repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create();
        componentContainer.initialise(new TramWithPostcodesEnabled());
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
    void shouldLoadPostcodes() {

        LatLong expected = TestEnv.nearWythenshaweHosp();

        PostcodeLocation result = repository.getPostcode(IdFor.createId(TestEnv.postcodeForWythenshaweHosp()));

        assertNotNull(result);
        LatLong position = result.getLatLong();
        Assertions.assertEquals(expected.getLat(), position.getLat(), 0.01);
        Assertions.assertEquals(expected.getLon(), position.getLon(), 0.01);
    }
}
