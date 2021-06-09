package com.tramchester.unit.graph;

import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.junit.jupiter.api.Test;
import org.neo4j.graphdb.Label;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class GraphLabelTest {

    @Test
    void shouldTestConversion() {
        List<Label> graphDBLabels = Arrays.asList(GraphLabel.BUS_STATION, GraphLabel.INTERCHANGE);

        Set<GraphLabel> result = GraphLabel.from(graphDBLabels);
        assertEquals(graphDBLabels.size(), result.size());

        assertTrue(result.contains(GraphLabel.BUS_STATION));
        assertTrue(result.contains(GraphLabel.INTERCHANGE));
    }

    @Test
    void shouldIdStationsCrude() {
        Set<GraphLabel> fromNames = Arrays.stream(GraphLabel.values()).
                filter(label -> label.name().toLowerCase().contains("station")).
                filter(label -> label!=GraphLabel.ROUTE_STATION).
                collect(Collectors.toSet());

        Set<GraphLabel> fromPredicate = Arrays.stream(GraphLabel.values()).filter(GraphLabel::isStation).collect(Collectors.toSet());

        fromNames.stream().filter(graphLabel -> graphLabel!=GraphLabel.TRAM_STATION).forEach(graphLabel ->  {
            assertTrue(GraphLabel.isNoPlatformStation(graphLabel), graphLabel.name());
        });

        assertEquals(fromNames, fromPredicate);
    }

    @Test
    void shouldHaveLabelForEachValidMode() {
        Set<TransportMode> modes = Arrays.stream(TransportMode.values()).
                filter(mode -> mode != TransportMode.Connect && mode != TransportMode.NotSet).collect(Collectors.toSet());
        for(TransportMode mode : modes) {
            assertNotNull(GraphLabel.forMode(mode));
        }
    }
}
