package com.tramchester.unit.graph;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.MapTransportRelationshipsToStages;
import com.tramchester.graph.Nodes.PlatformNode;
import com.tramchester.graph.Nodes.BoardPointNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Relationships.*;
import com.tramchester.repository.PlatformRepository;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.tramchester.domain.Station.METROLINK_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MapTransportRelationshipsToStagesTest extends EasyMockSupport {
    private MapTransportRelationshipsToStages mapper;
    private StationRepository stationRepository;
    List<TransportRelationship> relationships;
    private PlatformRepository platformRepository;
    private BoardPointNode boardingNode;
    private BoardPointNode departureNode;
    private Station stationA;
    private Station stationB;
    private RouteCodeToClassMapper routeCodeMapper;
    private String stationAId;
    private String stationBid;
    private StationNode stationNodeA;
    private StationNode stationNodeB;

    @Before
    public void beforeEachTestRuns() {
        stationAId = METROLINK_PREFIX + "statIdA";
        stationA = new Station(stationAId, "area1", "name1", new LatLong(0, 0), true);
        stationBid = METROLINK_PREFIX + "statIdB";
        stationB = new Station(stationBid, "area6", "name6", new LatLong(0, 0), true);

        stationNodeA = StationNode.TestOnly(stationAId, "name1");
        stationNodeB = StationNode.TestOnly(stationBid, "name2");

        boardingNode = BoardPointNode.TestOnly("id2", "routeNameA", "routeIdA", "stationName1");
        departureNode = BoardPointNode.TestOnly("id3", "routeNameA", "routeIdA", "stationName2");

        stationRepository = createMock(StationRepository.class);
        EasyMock.expect(stationRepository.getStation(stationAId)).andReturn(Optional.of(stationA));
        EasyMock.expect(stationRepository.getStation(stationBid)).andReturn(Optional.of(stationB));

        routeCodeMapper = createMock(RouteCodeToClassMapper.class);
        EasyMock.expect(routeCodeMapper.map("routeIdA")).andReturn("cssCodeAAA");

        platformRepository = createMock(PlatformRepository.class);
        MyLocationFactory myLocationFactory = new MyLocationFactory(new ObjectMapper());

        mapper = new MapTransportRelationshipsToStages(routeCodeMapper, stationRepository, platformRepository, myLocationFactory);

        relationships = new LinkedList<>();
    }

    @Test
    public void shouldMapSimpleJourney() {

        boolean[] daysRunning = new boolean[1];
        LocalTime[] timesRunning = new LocalTime[1];

        relationships.add(BoardRelationship.TestOnly("BoardRelId", stationNodeA, boardingNode));

        TramServiceDate startDate = new TramServiceDate(LocalDate.now());
        TramServiceDate endDate = new TramServiceDate(LocalDate.now());

        String tripId = "";
        relationships.add(TramGoesToRelationship.TestOnly("svcId", 18, daysRunning, timesRunning, "id4", startDate,
                endDate, "dest", boardingNode, departureNode, tripId));

        relationships.add(DepartRelationship.TestOnly("depRelId", departureNode, stationNodeB));

        replayAll();
        List<RawStage> result = mapper.mapStages(relationships, LocalTime.of(7,0));
        verifyAll();

        assertEquals(1, result.size());
        RawVehicleStage stage = (RawVehicleStage) result.get(0);
        assertNotNull(stage);
        assertEquals( stationA, stage.getFirstStation());
        assertEquals( stationB, stage.getLastStation());
        assertEquals( stationA, stage.getActionStation());
        assertEquals( "cssCodeAAA", stage.getDisplayClass());
        assertEquals(TransportMode.Tram, stage.getMode());
        assertEquals( "svcId", stage.getServiceId());
        assertEquals(0, stage.getPassedStops());
    }

    @Test
    public void shouldMapSimpleJourneyWithPlatform() {
        String enterPlatformId = stationA.getId() + "1";
        String leavePlatformId = stationB.getId() + "2";

        TramServiceDate startDate = new TramServiceDate(LocalDate.now());
        TramServiceDate endDate = new TramServiceDate(LocalDate.now());

        PlatformNode enterPlatformNode = PlatformNode.TestOnly(enterPlatformId, "node name plat 1");
        PlatformNode leavePlatformNode = PlatformNode.TestOnly(leavePlatformId, "node name plat 2");

        boolean[] daysRunning = new boolean[1];
        LocalTime[] timesRunning = new LocalTime[1];

        // station -> platform
        relationships.add(EnterPlatformRelationship.TestOnly(0, "entPlatRId", stationNodeA, enterPlatformNode));

        // platform -> borading point
        relationships.add(BoardRelationship.TestOnly("boardRelId", enterPlatformNode, boardingNode));

        // boarding point -> next boarding point
        String tripId = "";
        relationships.add(TramGoesToRelationship.TestOnly("svcId", 18, daysRunning, timesRunning, "id4", startDate,
                endDate, "dest", boardingNode, departureNode, tripId));

        // boarding point -> platform
        relationships.add(DepartRelationship.TestOnly("depRelId", departureNode,
                leavePlatformNode));

        // platform -> station
        relationships.add(LeavePlatformRelationship.TestOnly(0, "leavePlatRid", leavePlatformNode, stationNodeB));

        Platform platformA = new Platform(enterPlatformId, "platAName");

        EasyMock.expect(platformRepository.getPlatformById(enterPlatformId)).andReturn(Optional.of(platformA));

        replayAll();
        List<RawStage> result = mapper.mapStages(relationships, LocalTime.of(7,0));
        verifyAll();

        assertEquals(1, result.size());
        RawVehicleStage stage = (RawVehicleStage) result.get(0);
        assertNotNull(stage);
        assertEquals( stationA, stage.getFirstStation());
        assertEquals( stationB, stage.getLastStation());
        assertEquals( stationA, stage.getActionStation());
        assertEquals( "cssCodeAAA", stage.getDisplayClass());
        assertEquals(TransportMode.Tram, stage.getMode());
        assertEquals( "svcId", stage.getServiceId());
        assertTrue( stage.getBoardingPlatform().isPresent());
        assertEquals(platformA, stage.getBoardingPlatform().get());
    }

}
