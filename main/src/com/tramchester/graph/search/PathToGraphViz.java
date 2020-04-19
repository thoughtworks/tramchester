package com.tramchester.graph.search;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

public class PathToGraphViz {

    private static String prefixToRemove = Station.METROLINK_PREFIX + "MA";

    public static Set<RenderLater> map(Path path, ServiceReason serviceReason, boolean valid) {
        Set<RenderLater> nodes = new HashSet<>();
        Set<RenderLater> edges = new HashSet<>();
        Iterable<Relationship> relationships = path.relationships();

        long lastNodeInPath = path.endNode().getId();

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

                String relationshipType = relationship.getType().name();
                String shape = valid ? "box" : "oval";
                nodes.add(RenderLater.now(format("\"%s\" [shape=%s];\n", endId, shape)));

//                if  (serviceReason.isValid()) {
//                    edges.add(RenderLater.now(format("\"%s\"->\"%s\" [label=\"%s\"];\n", startId, endId, relationshipType)));
//                } else {
//                    edges.add(RenderLater.later(startId, endId, relationshipType, serviceReason));
//                }

//                if (endNode.getId()!=lastNodeInPath) {
//                    edges.add(RenderLater.now(format("\"%s\"->\"%s\" [label=\"%s\"];\n", startId, endId, relationshipType)));
//                } else {
//                    edges.add(RenderLater.later(startId, endId, relationshipType, serviceReason));
//                }

                if (endNode.getId()==lastNodeInPath) {
                    edges.add(RenderLater.later(startId, endId, relationshipType, serviceReason));
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

    public static class RenderLater {
        private final String value;
        private final ServiceReason serviceReason;

        private RenderLater(String value) {
            this.value = value;
            serviceReason = null;
        }

        public RenderLater(String value, ServiceReason serviceReason) {
            this.value = value;
            this.serviceReason = serviceReason;
        }

        public static RenderLater now(String value) {
            return new RenderLater(value);
        }

        public static RenderLater later(String startId, String endId, String relat, ServiceReason serviceReason) {
            // second half of string rendered in render()
            return new RenderLater(format("\"%s\"->\"%s\" [label=\"%s", startId, endId, relat), serviceReason);

        }

        public String render() {
            if (serviceReason==null) {
                return value;
            }
            return format("%s %s\"];\n", value, serviceReason.textForGraph());
        }
    }
}
