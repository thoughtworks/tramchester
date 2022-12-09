package com.tramchester.integration.repository.neighbours;

import com.tramchester.ComponentsBuilder;
import com.tramchester.GuiceContainerDependencies;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.StationLink;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.Station;
import com.tramchester.integration.testSupport.RailAndTramGreaterManchesterConfig;
import com.tramchester.integration.testSupport.rail.RailStationIds;
import com.tramchester.repository.NeighboursRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NeighboursRepositoryTrainTramTest {

    private static GuiceContainerDependencies componentContainer;
    private NeighboursRepository neighboursRepository;
    private Station altrinchamTram;
    private Station altrinchamTrain;
    private StationRepository stationRepository;

    @BeforeAll
    static void onceBeforeAnyTestsRun() {
        TramchesterConfig config = new RailAndTramGreaterManchesterConfig();

        componentContainer = new ComponentsBuilder().create(config, TestEnv.NoopRegisterMetrics());
        componentContainer.initialise();
    }

    @AfterAll
    static void OnceAfterAllTestsAreFinished() {
        componentContainer.close();
    }

    @BeforeEach
    void onceBeforeEachTestRuns() {
        neighboursRepository = componentContainer.get(NeighboursRepository.class);
        stationRepository = componentContainer.get(StationRepository.class);

        altrinchamTram = TramStations.Altrincham.from(stationRepository);
        altrinchamTrain = RailStationIds.Altrincham.from(stationRepository);
    }

    @Test
    void shouldHaveNeighbouringTrainAndTramAtAltrincham() {
        assertTrue(neighboursRepository.hasNeighbours(TramStations.Altrincham.getId()));
        assertTrue(neighboursRepository.hasNeighbours(RailStationIds.Altrincham.getId()));
    }

    @Test
    void shouldHaveExpectedNeighboursForTram() {
        Set<Station> neighboursForTram = neighboursRepository.getNeighboursFor(TramStations.Altrincham.getId());
        assertEquals(1, neighboursForTram.size(), HasId.asIds(neighboursForTram));

        assertTrue(neighboursForTram.contains(altrinchamTrain), HasId.asIds(neighboursForTram));
    }

    @Test
    void shouldHaveExpectedNeighboursForTrain() {
        Set<Station> neighboursForTrain = neighboursRepository.getNeighboursFor(RailStationIds.Altrincham.getId());
        assertEquals(1, neighboursForTrain.size(),  HasId.asIds(neighboursForTrain));

        assertTrue(neighboursForTrain.contains(altrinchamTram), HasId.asIds(neighboursForTrain));
    }

    @Test
    void shouldHaveNeighbourCheck() {
        assertTrue(neighboursRepository.areNeighbours(altrinchamTrain, altrinchamTram));
        assertTrue(neighboursRepository.areNeighbours(altrinchamTram, altrinchamTrain));

        Station otherStation = TramStations.NavigationRoad.from(stationRepository);

        assertFalse(neighboursRepository.areNeighbours(altrinchamTrain, otherStation));
        assertFalse(neighboursRepository.areNeighbours(otherStation, altrinchamTrain));
    }

    @Test
    void shouldHaveLinksForTrainStation() {
        List<StationLink> linksForTrain = new ArrayList<>(neighboursRepository.getNeighbourLinksFor(altrinchamTrain.getId()));
        assertEquals(1, linksForTrain.size());

        StationLink foundLink = linksForTrain.get(0);

        assertEquals(altrinchamTrain, foundLink.getBegin());
        assertEquals(altrinchamTram, foundLink.getEnd());
    }

    @Test
    void shouldHaveLinksForTramStation() {
        List<StationLink> linksForTram = new ArrayList<>(neighboursRepository.getNeighbourLinksFor(altrinchamTram.getId()));
        assertEquals(1, linksForTram.size());

        StationLink foundLink = linksForTram.get(0);

        assertEquals(altrinchamTram, foundLink.getBegin());
        assertEquals(altrinchamTrain, foundLink.getEnd());
    }

    @Test
    void shouldGetAllLinksAsExpected() {
        Set<StationLink> allLinks = neighboursRepository.getAll();

        List<StationLink> fromTram = allLinks.stream().filter(link -> link.getBegin().equals(altrinchamTram)).collect(Collectors.toList());
        assertEquals(1, fromTram.size());
        assertEquals(altrinchamTrain, fromTram.get(0).getEnd());

        List<StationLink> fromTrain = allLinks.stream().filter(link -> link.getBegin().equals(altrinchamTrain)).collect(Collectors.toList());
        assertEquals(1, fromTrain.size());
        assertEquals(altrinchamTram, fromTrain.get(0).getEnd());
    }
}
