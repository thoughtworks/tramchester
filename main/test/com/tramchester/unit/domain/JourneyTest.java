package com.tramchester.unit.domain;

import com.tramchester.domain.*;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.domain.time.TramTime;
import com.tramchester.domain.transportStages.VehicleStage;
import com.tramchester.testSupport.TestEnv;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.tramchester.domain.reference.TransportMode.*;
import static org.junit.jupiter.api.Assertions.*;

class JourneyTest extends EasyMockSupport {

    private TramTime queryTime;
    private List<Location<?>> path;
    private List<TransportStage<?,?>> stages;

    @BeforeEach
    void beforeEachTest() {
        queryTime = TramTime.of(9,16);
        path = Collections.emptyList();
        stages = new ArrayList<>();
    }

    @Test
    void shouldHaveExpecetdTransportModesSingle() {
        stages.add(stageExpectionsFor(Tram));
        Journey journey = new Journey(stages, queryTime, path);

        replayAll();
        Set<TransportMode> result = journey.getTransportModes();
        verifyAll();

        assertEquals(1, result.size());
        assertTrue(result.contains(Tram));
    }

    @Test
    void shouldHaveExpecetdTransportModesMultiple() {
        stages.add(stageExpectionsFor(Bus));
        stages.add(stageExpectionsFor(Train));

        Journey journey = new Journey(stages, queryTime, path);

        replayAll();
        Set<TransportMode> result = journey.getTransportModes();
        verifyAll();

        assertEquals(2, result.size());
        assertTrue(result.contains(Bus));
        assertTrue(result.contains(Train));
        assertFalse(result.contains(Tram));
    }

    @Test
    void shouldHaveCallingPlatformIds() {
        stages.add(stageExpectionsFor("platformId1"));
        stages.add(stageExpectionsFor("platformId2"));

        TransportStage<?, ?> noPlatformStage = createMock(VehicleStage.class);
        EasyMock.expect(noPlatformStage.hasBoardingPlatform()).andReturn(false);

        stages.add(noPlatformStage);

        Journey journey = new Journey(stages, queryTime, path);

        replayAll();
        IdSet<Platform> result = journey.getCallingPlatformIds();
        verifyAll();

        assertEquals(2, result.size());
        assertTrue(result.contains(StringIdFor.createId("platformId1")));
        assertTrue(result.contains(StringIdFor.createId("platformId2")));

    }

    private VehicleStage stageExpectionsFor(TransportMode mode) {
        VehicleStage mockStage = createMock(VehicleStage.class);
        EasyMock.expect(mockStage.getMode()).andReturn(mode);
        return mockStage;
    }

    private VehicleStage stageExpectionsFor(String platformId) {
        VehicleStage mockStage = createMock(VehicleStage.class);
        Platform platform = new Platform(platformId, "platformName"+platformId, TestEnv.nearGreenwich);
        EasyMock.expect(mockStage.hasBoardingPlatform()).andReturn(true);
        EasyMock.expect(mockStage.getBoardingPlatform()).andReturn(platform);
        return mockStage;
    }
}
