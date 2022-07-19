package com.tramchester.integration.graph.railAndTram;

import com.tramchester.ComponentContainer;
import com.tramchester.ComponentsBuilder;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.InterchangeStation;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.TramAndTrainGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.InterchangeRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.AdditionalTramInterchanges;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.testTags.TrainTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TrainTest
public class RailAndTramInterchangeTest {

        private static ComponentContainer componentContainer;
        private InterchangeRepository interchangeRepository;
        private StationRepository stationRepository;

        @BeforeAll
        static void onceBeforeAnyTestsRun() {
            componentContainer = new ComponentsBuilder().create(new TramAndTrainGreaterManchesterConfig(), TestEnv.NoopRegisterMetrics());
            componentContainer.initialise();
        }

        @AfterAll
        static void OnceAfterAllTestsAreFinished() {
            componentContainer.close();
        }

        @BeforeEach
        void onceBeforeEachTestRuns() {
            stationRepository = componentContainer.get(StationRepository.class);
            interchangeRepository = componentContainer.get(InterchangeRepository.class);
        }

        @Test
        void shouldHaveAdditionalTramInterchanges() {
            for (IdFor<Station> interchangeId : AdditionalTramInterchanges.stations()) {
                Station interchange = stationRepository.getStationById(interchangeId);
                assertTrue(interchangeRepository.isInterchange(interchange), interchange.toString());
            }
        }

        @Test
        void shouldHaveAltrinchamRailAsInterchange() {
            Station altrincham = RailStationIds.Altrincham.from(stationRepository);
            boolean result = interchangeRepository.isInterchange(altrincham);
            assertTrue(result);

            InterchangeStation interchange = interchangeRepository.getInterchange(altrincham);

            assertTrue(interchange.isMultiMode());
        }
    }

