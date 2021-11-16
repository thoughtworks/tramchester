package com.tramchester.unit.domain;

import com.tramchester.domain.Agency;
import com.tramchester.domain.id.StringIdFor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgencyTest {

    @Test
    void shouldIdMetrolinkAgency() {
        // New ID
        assertTrue(Agency.IsMetrolink(StringIdFor.createId("METL")));

        // Old ID
        assertFalse(Agency.IsMetrolink(StringIdFor.createId("MET")));

    }
}
