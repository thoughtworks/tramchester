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
        List<Label> graphDBLabels = Arrays.asList(STATION, INTERCHANGE);

        Set<GraphLabel> result = GraphLabel.from(graphDBLabels);
        assertEquals(graphDBLabels.size(), result.size());

        assertTrue(result.contains(STATION));
        assertTrue(result.contains(INTERCHANGE));
    }

    @Test
    void shouldHaveLabelForEachValidMode() {
        Set<TransportMode> modes = Arrays.stream(TransportMode.values()).
                filter(mode -> mode != TransportMode.Connect).
                filter(mode -> mode != TransportMode.NotSet).
                filter(mode -> mode != TransportMode.Unknown).
                collect(Collectors.toSet());
        for(TransportMode mode : modes) {
            assertNotNull(GraphLabel.forMode(mode));
        }
    }

}
