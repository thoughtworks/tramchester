package com.tramchester.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.*;
import com.tramchester.repository.TransportDataFromFiles;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class GenericJourneyResponseMapperTest extends EasyMockSupport {

    public static final int AM8 = 8 * 60;
    private GenericJourneyResponseMapper mapper;
    private TransportDataFromFiles transportData;
    Set<RawJourney> rawJourneys;
    List<TransportStage> stages;
    private Station stationA;
    private Station stationB;
    private Station stationC;

    @Before
    public void beforeEachTestRuns() {
        rawJourneys = new HashSet<>();
        stages = new LinkedList<>();

        stationA = new Station("stationA", "area", "nameA", -2, -1, false);
        stationB = new Station("stationB", "area", "nameA", -3, 1, false);
        stationC = new Station("stationC", "area", "nameA", -4, 2, false);

        transportData = createMock(TransportDataFromFiles.class);
        mapper = new GenericJourneyResponseMapper(transportData);
    }

    @Test
    public void testSimpleMappingOfJourney() throws TramchesterException {

        createSimpleRawJourney(AM8 + 2, AM8 + 9);

        EasyMock.expect(transportData.getStation("stationA")).andStubReturn(stationA);
        EasyMock.expect(transportData.getStation("stationB")).andStubReturn(stationB);
        EasyMock.expect(transportData.getStation("stationC")).andStubReturn(stationC);

        SortedSet<ServiceTime> timesLeg1 = new TreeSet<>();
        timesLeg1.add(new ServiceTime(LocalTime.of(8,2), LocalTime.of(8,9), "svcId", "headSign", "tripIdA"));
        timesLeg1.add(new ServiceTime(LocalTime.of(8,3), LocalTime.of(8,11), "svcId", "headSign", "tripIdA"));

        SortedSet<ServiceTime> timesLeg2 = new TreeSet<>();
        timesLeg2.add(new ServiceTime(LocalTime.of(8,9), LocalTime.of(8,18), "svcId", "headSign", "tripIdA"));
        timesLeg2.add(new ServiceTime(LocalTime.of(8,11), LocalTime.of(8,20), "svcId", "headSign", "tripIdA"));

        EasyMock.expect(transportData.getTimes("svcId", stationA, stationB, new TimeWindow(AM8+2, 30))).andReturn(timesLeg1);
        EasyMock.expect(transportData.getTimes("svcId", stationB, stationC, new TimeWindow(AM8+9, 30))).andReturn(timesLeg2);

        replayAll();
        JourneyPlanRepresentation result = mapper.map(rawJourneys, new TimeWindow(AM8, 30));

        assertEquals(rawJourneys.size(), result.getJourneys().size());

        Set<Journey> journeys = result.getJourneys();
        assertEquals(1, journeys.size());

        Journey found = journeys.iterator().next();

        assertEquals(LocalTime.of(8,18),found.getExpectedArrivalTime());
        assertEquals(LocalTime.of(8,2),found.getFirstDepartureTime());
        PresentableStage stageFirst = found.getStages().get(0);
        PresentableStage stageSecond = found.getStages().get(1);
        assertEquals(7, stageFirst.getDuration());
        assertEquals(9, stageSecond.getDuration());
        verifyAll();
    }

    @Test
    public void testSimpleMappingOfJourneyUseEarliestArrive() throws TramchesterException {

        createSimpleRawJourney(AM8 + 2, AM8 + 7);

        EasyMock.expect(transportData.getStation("stationA")).andStubReturn(stationA);
        EasyMock.expect(transportData.getStation("stationB")).andStubReturn(stationB);
        EasyMock.expect(transportData.getStation("stationC")).andStubReturn(stationC);

        SortedSet<ServiceTime> timesLeg1 = new TreeSet<>();
        timesLeg1.add(new ServiceTime(LocalTime.of(8,2), LocalTime.of(8,9), "svcId", "headSign", "tripIdA"));
        timesLeg1.add(new ServiceTime(LocalTime.of(8,3), LocalTime.of(8,7), "svcId", "headSign", "tripIdA"));

        SortedSet<ServiceTime> timesLeg2 = new TreeSet<>();
        timesLeg2.add(new ServiceTime(LocalTime.of(8,7), LocalTime.of(8,16), "svcId", "headSign", "tripIdA"));
        timesLeg2.add(new ServiceTime(LocalTime.of(8,9), LocalTime.of(8,18), "svcId", "headSign", "tripIdA"));

        EasyMock.expect(transportData.getTimes("svcId", stationA, stationB, new TimeWindow(AM8+2, 30))).andReturn(timesLeg1);
        EasyMock.expect(transportData.getTimes("svcId", stationB, stationC, new TimeWindow(AM8+7, 30))).andReturn(timesLeg2);

        replayAll();
        JourneyPlanRepresentation result = mapper.map(rawJourneys, new TimeWindow(AM8, 30));

        assertEquals(rawJourneys.size(), result.getJourneys().size());

        Set<Journey> journeys = result.getJourneys();
        assertEquals(1, journeys.size());

        Journey found = journeys.iterator().next();

        assertEquals(LocalTime.of(8,16),found.getExpectedArrivalTime());
        assertEquals(LocalTime.of(8,3),found.getFirstDepartureTime());
        PresentableStage stageFirst = found.getStages().get(0);
        PresentableStage stageSecond = found.getStages().get(1);
        assertEquals(4, stageFirst.getDuration());
        assertEquals(9, stageSecond.getDuration());
        verifyAll();

        assertEquals("Board bus at", stageFirst.getPrompt());
        assertEquals("Change bus at", stageSecond.getPrompt());
    }

    private void createSimpleRawJourney(int stageATime, int stageBTime) {
        RawVehicleStage rawTravelStage1 = new RawVehicleStage(stationA, "routeNameA", TransportMode.Bus, "routeIdA", stageATime).
                setLastStation(stationB).setServiceId("svcId");
        RawVehicleStage rawTravelStage2 = new RawVehicleStage(stationB, "routeNameA", TransportMode.Bus, "routeIdA", stageBTime).
                setLastStation(stationC).setServiceId("svcId");
        stages.add(rawTravelStage1);
        stages.add(rawTravelStage2);

        RawJourney rawJourney = new RawJourney(stages);
        rawJourneys.add(rawJourney);
    }

}
