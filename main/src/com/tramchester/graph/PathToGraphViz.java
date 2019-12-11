package com.tramchester.graph;

import com.tramchester.domain.Station;
import org.neo4j.graphdb.Node;
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

        long lastId = path.endNode().getId();

        if (relationships!=null) {
            relationships.forEach(relationship -> {
                Node startNode = relationship.getStartNode();
                String startId = getId(startNode);
                if (hasTrip(startNode)) {
                    startId = startId + addTripId(startNode);
                }

                Node endNode = relationship.getEndNode();
                String endId = getId(endNode);
                if (hasTrip(endNode)) {
                    endId = endId + addTripId(endNode);
                }

                String relat = relationship.getType().name();
                String shape = valid ? "box" : "oval";
                nodes.add(format("\"%s\" [shape=%s];\n", endId, shape));
                if (!(endNode.getId()==lastId)) {
                    edges.add(format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                            startId, endId, relat));
                } else {
                    edges.add(format("\"%s\"->\"%s\" [label=\"%s %s\"];\n",
                            startId, endId, relat, diagnostics));
                }
            });
        }

        nodes.addAll(edges);
        return nodes;
    }

    private static String addTripId(Node endNode) {
        return "_" + endNode.getProperty(GraphStaticKeys.TRIP_ID);
    }

    private static boolean hasTrip(Node endNode) {
        return endNode.hasProperty(GraphStaticKeys.TRIP_ID);
    }

    private static String getId(Node node) {
        return node.getProperty(GraphStaticKeys.ID).toString().replace(prefixToRemove, "");
    }
}
