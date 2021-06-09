package com.tramchester.unit.domain.factory;

import com.tramchester.domain.id.StringIdFor;
import org.junit.jupiter.api.Test;

import static com.tramchester.domain.factory.TransportEntityFactoryForTFGM.getStationIdFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransportEntityFactoryForTFGMTest {

    @Test
    void testShouldFormIdByRemovingPlatformForTramStopIfRequired() {
        assertEquals(StringIdFor.createId("9400ZZid"), getStationIdFor("9400ZZid1"));

        assertEquals(StringIdFor.createId("9400XXid1"), getStationIdFor("9400XXid1"));

    }
}
