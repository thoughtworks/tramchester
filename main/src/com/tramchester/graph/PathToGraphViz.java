package com.tramchester.graph;

import com.tramchester.domain.Station;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public class PathToGraphViz {

    private static String prefixToRemove = Station.METROLINK_PREFIX + "MA";

    public static Set<String> map(Path path, String diagnostics, boolean valid) {
        Set<String> nodes = new HashSet<>();
        Set<String> edges = new HashSet<>();
        Iterable<Relationship> relationships = path.relationships();

        if (relationships!=null) {
            relationships.forEach(relationship -> {
                String start = relationship.getStartNode().getProperty(GraphStaticKeys.ID).toString().
                        replace(prefixToRemove, "");
                String end = relationship.getEndNode().getProperty(GraphStaticKeys.ID).toString().
                        replace(prefixToRemove, "");
                String relat = relationship.getType().name();
                String shape = valid ? "box" : "oval";
                nodes.add(format("\"%s\" [shape=%s];\n", end, shape));
                edges.add(format("\"%s\"->\"%s\" [label=\"%s %s\"];\n",
                        start, end, relat, diagnostics));

            });
        }

        nodes.addAll(edges);
        return nodes;
    }
}
