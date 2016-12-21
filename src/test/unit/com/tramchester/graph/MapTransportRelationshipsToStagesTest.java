package com.tramchester.graph;


import com.tramchester.domain.RawStage;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.Station;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Relationships.BoardRelationship;
import com.tramchester.graph.Relationships.DepartRelationship;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MapTransportRelationshipsToStagesTest extends EasyMockSupport {
    private MapTransportRelationshipsToStages mapper;
    private StationRepository stationRepository;

    @Before
    public void beforeEachTestRuns() {
        stationRepository = createMock(StationRepository.class);
        mapper = new MapTransportRelationshipsToStages(new RouteCodeToClassMapper(), stationRepository);
    }

    @Test
    public void shouldMapSimpleJourney() {
        EasyMock.expect(stationRepository.getStation("id1")).
                andReturn(Optional.of(new Station("id1","area1","name1", new LatLong(0,0),true)));
        EasyMock.expect(stationRepository.getStation("id6")).
                andReturn(Optional.of(new Station("id6","area6","name6", new LatLong(0,0),true)));

        List<TransportRelationship> relationships = new LinkedList<>();
        RouteStationNode boardingPoint = RouteStationNode.TestOnly("id2", "routeNameA", "routeIdA", "stationName1");
        RouteStationNode departurePoint = RouteStationNode.TestOnly("id3", "routeNameA", "routeIdA", "stationName2");

        relationships.add(BoardRelationship.TestOnly(2, "someId",
                StationNode.TestOnly("id1", "name1"),
                boardingPoint));

        boolean[] daysRunning = new boolean[1];
        int[] timesRunning = new int[1];
        String id = "id4";
        TramServiceDate startDate = new TramServiceDate(LocalDate.now());
        TramServiceDate endDate = new TramServiceDate(LocalDate.now());
        relationships.add(TramGoesToRelationship.TestOnly("svcId", 18, daysRunning, timesRunning, id, startDate,
                endDate, "dest", boardingPoint, departurePoint));

        relationships.add(DepartRelationship.TestOnly(2,"id5",departurePoint, StationNode.TestOnly("id6","name2")));

        replayAll();
        List<RawStage> result = mapper.mapStages(relationships, 7 * 60);
        verifyAll();

        assertEquals(1, result.size());
        RawVehicleStage stage = (RawVehicleStage) result.get(0);
        assertNotNull(stage);
    }

}
