package com.tramchester.integration.graph;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.graph.search.routes.InterchangeMatrix;
import com.tramchester.integration.testSupport.ConfigParameterResolver;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.DualTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.tramchester.testSupport.reference.TramStations.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ConfigParameterResolver.class)
@DualTest
public class InterchangeMatrixTest {
    private static ComponentContainer componentContainer;

    //private TramDate date;
    //private EnumSet<TransportMode> modes;
    private InterchangeMatrix matrix;

    // NOTE: this test does not cause a full db rebuild, so might see VERSION node missing messages

    @BeforeAll
    static void onceBeforeAnyTestRuns(TramchesterConfig tramchesterConfig) {

        componentContainer = new ComponentsBuilder().create(tramchesterConfig, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

        TestEnv.clearDataCache(componentContainer);
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        TestEnv.clearDataCache(componentContainer);
        componentContainer.close();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        matrix = componentContainer.get(InterchangeMatrix.class);

//        date = TestEnv.testDay();
//        modes = EnumSet.of(Tram);
    }

    @Test
    void shouldHaveDirectlyLinked() {
        assertEquals(0, matrix.getDegree(StPetersSquare.getId(), Cornbrook.getId()));
        assertEquals(0, matrix.getDegree(Cornbrook.getId(), StPetersSquare.getId()));
    }

    @Test
    void shouldHaveIndirectlyLinkedStWerburghsRoadPomona() {
        assertEquals(1, matrix.getDegree(StWerburghsRoad.getId(), Pomona.getId()));
        assertEquals(1, matrix.getDegree(Pomona.getId(), StWerburghsRoad.getId()));
    }

    @Test
    void shouldHaveIndirectlyLinkedStWerburghsRoadPiccadilly() {
        assertEquals(1, matrix.getDegree(StWerburghsRoad.getId(), Piccadilly.getId()));
        assertEquals(1, matrix.getDegree(Piccadilly.getId(), StWerburghsRoad.getId()));
    }
}
