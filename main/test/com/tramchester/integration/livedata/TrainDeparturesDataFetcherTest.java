package com.tramchester.integration.livedata;

import com.thalesgroup.rtti._2017_10_01.ldb.types.ServiceItem;
import com.thalesgroup.rtti._2017_10_01.ldb.types.StationBoard;
import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.livedata.openLdb.TrainDeparturesDataFetcher;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TrainDeparturesDataFetcherTest {
    private static GuiceContainerDependencies componentContainer;
    private TrainDeparturesDataFetcher dataFetcher;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        componentContainer = new ComponentsBuilder().create(new TramAndTrainGreaterManchesterConfig(),
                TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();

    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTest() {
        dataFetcher = componentContainer.get(TrainDeparturesDataFetcher.class);
        stationRepository = componentContainer.get(StationRepository.class);
    }

    @Test
    void testShouldGetDeparturesForStation() {
        StationBoard board = dataFetcher.getFor(RailStationIds.ManchesterPiccadilly.from(stationRepository));
        assertEquals("MAN", board.getCrs());

        List<ServiceItem> services = board.getTrainServices().getService();

        assertFalse(services.isEmpty());

    }

}
