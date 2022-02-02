package com.tramchester.unit.geo;

import com.tramchester.domain.BoxWithServiceFrequency;
import com.tramchester.domain.MutableService;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.MutableTrip;
import com.tramchester.domain.input.NoPlatformStopCall;
import com.tramchester.domain.input.StopCall;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.GTFSPickupDropoffType;
import com.tramchester.domain.time.TramTime;
import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.BoundingBoxWithStations;
import com.tramchester.geo.StationLocations;
import com.tramchester.geo.StopCallsForGrid;
import com.tramchester.repository.StopCallRepository;
import com.tramchester.testSupport.TestEnv;
import com.tramchester.testSupport.reference.TramStations;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.domain.id.StringIdFor.createId;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StopCallsForGridTest extends EasyMockSupport {

    StopCallsForGrid stopCallsForGrid;
    private StationLocations stationLocations;
    private StopCallRepository stopCallRepository;
    private Trip trip;

    @BeforeEach
    void beforeEachTest() {

        Service service = MutableService.build(createId("serviceZZZ"));
        trip = MutableTrip.build(createId("trip999"), "headsign", service, TestEnv.getTramTestRoute());

        stationLocations = createMock(StationLocations.class);
        stopCallRepository = createMock(StopCallRepository.class);
        stopCallsForGrid = new StopCallsForGrid(stationLocations, stopCallRepository);
    }

    @Test
    void testGetStopcallsForGrid() {
        LocalDate date = TestEnv.testDay();
        TramTime begin = TramTime.of(11,30);
        TramTime end = TramTime.of(13,45);

        final Station station1 = TramStations.Altrincham.fake();
        final Station station2 = TramStations.Bury.fake();
        final Station station3 = TramStations.Anchorage.fake();

        Set<Station> gridAStations = new HashSet<>();
        gridAStations.add(station1);
        gridAStations.add(station2);
        Set<Station> gridBStations = new HashSet<>();
        gridBStations.add(station3);

        Set<StopCall> station1Calls = createStopCalls(station1, begin, 10,3);
        Set<StopCall> station2Calls = createStopCalls(station2, begin, 10, 3);
        Set<StopCall> station3Calls = createStopCalls(station3, begin, 5, 10);

        List<BoundingBoxWithStations> boundingBoxes = new ArrayList<>();

        final BoundingBox boxA = new BoundingBox(1, 1, 2, 2);
        final BoundingBox boxB = new BoundingBox(3, 3, 4, 4);

        boundingBoxes.add(new BoundingBoxWithStations(boxA, gridAStations));
        boundingBoxes.add(new BoundingBoxWithStations(boxB, gridBStations));

        final int gridSize = 2000;
        EasyMock.expect(stationLocations.getStationsInGrids(gridSize)).andReturn(boundingBoxes.stream());
        EasyMock.expect(stopCallRepository.getStopCallsFor(station1, date, begin, end)).andReturn(station1Calls);
        EasyMock.expect(stopCallRepository.getStopCallsFor(station2, date, begin, end)).andReturn(station2Calls);
        EasyMock.expect(stopCallRepository.getStopCallsFor(station3, date, begin, end)).andReturn(station3Calls);

        replayAll();
        Set<BoxWithServiceFrequency> results = stopCallsForGrid.
                getServiceFreqencies(gridSize, date, begin, end).collect(Collectors.toSet());
        verifyAll();

        assertEquals(2, results.size());

        List<BoxWithServiceFrequency> forBoxA = results.stream().filter(result -> result.overlapsWith(boxA)).collect(Collectors.toList());
        assertEquals(1, forBoxA.size());
        assertEquals(3+3, forBoxA.get(0).getNumberOfStopcalls());

        List<BoxWithServiceFrequency> forBoxB = results.stream().filter(result -> result.overlapsWith(boxB)).collect(Collectors.toList());
        assertEquals(1, forBoxB.size());
        assertEquals(10, forBoxB.get(0).getNumberOfStopcalls());
    }

    private Set<StopCall> createStopCalls(Station station, TramTime first, int intervalMins, int number) {
        Set<StopCall> stopCalls = new HashSet<>();

        for (int i = 0; i < number; i++) {
            TramTime arrivalTime = first.plusMinutes(intervalMins*number);
            NoPlatformStopCall stopCall = new NoPlatformStopCall(station, arrivalTime, arrivalTime.plusMinutes(1),
                    i, GTFSPickupDropoffType.Regular, GTFSPickupDropoffType.Regular, trip);
            stopCalls.add(stopCall);
        }
        return stopCalls;
    }
}
