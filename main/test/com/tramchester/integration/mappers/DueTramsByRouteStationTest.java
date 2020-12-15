package com.tramchester.integration.mappers;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.integration.testSupport.IntegrationTramTestConfig;
import com.tramchester.mappers.DueTramsByRouteStation;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DueTramsByRouteStationTest {
    private static ComponentContainer componentContainer;
    private DueTramsByRouteStation repository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        IntegrationTramTestConfig testConfig = new IntegrationTramTestConfig();
//        componentContainer = new ComponentsBuilder<>().create(testConfig, TestEnv.NoopRegisterMetrics());
//        componentContainer.initialise();
    }

    @BeforeEach
    void beforeEachTestRuns() {
        //repository = componentContainer.get(DueTramsByRouteStation.class);
    }

    @Test
    void shouldGetSubsetForStPetersSquare() {
        // WIP
    }
}
