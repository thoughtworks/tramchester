package com.tramchester.unit.graph;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tramchester.graph.graphbuild.GraphLabel.*;
import static org.junit.jupiter.api.Assertions.*;

public class GraphLabelTest {

    @Test
    void shouldTestConversion() {
        List<Label> graphDBLabels = Arrays.asList(BUS_STATION, INTERCHANGE);

        Set<GraphLabel> result = GraphLabel.from(graphDBLabels);
        assertEquals(graphDBLabels.size(), result.size());

        assertTrue(result.contains(BUS_STATION));
        assertTrue(result.contains(INTERCHANGE));
    }

    @Test
    void shouldHaveLabelForEachValidMode() {
        Set<TransportMode> modes = Arrays.stream(TransportMode.values()).
                filter(mode -> mode != TransportMode.Connect && mode != TransportMode.NotSet).collect(Collectors.toSet());
        for(TransportMode mode : modes) {
            assertNotNull(GraphLabel.forMode(mode));
        }
    }

    @Test
    void shouldIdStationNodes() {
        assertFalse(GraphLabel.isStation(Arrays.asList(BUS_STATION, INTERCHANGE)));

        assertTrue(GraphLabel.isStation(Arrays.asList(BUS_STATION, STATION)));
    }
}
