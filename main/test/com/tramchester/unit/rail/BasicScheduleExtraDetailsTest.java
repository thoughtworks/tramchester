package com.tramchester.unit.rail;

import com.tramchester.dataimport.rail.records.BasicScheduleExtraDetails;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BasicScheduleExtraDetailsTest {
    @Test
    void shouldParseCorrectly() {
        String line = "BX         EMYEM813500";

        BasicScheduleExtraDetails extraDetails = BasicScheduleExtraDetails.parse(line);

        assertEquals("EM", extraDetails.getAtocCode());
        assertEquals("EM813500", extraDetails.getRetailServiceID());
    }
}
