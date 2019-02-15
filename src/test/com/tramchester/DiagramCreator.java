package com.tramchester;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.*;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.graph.TransportGraphBuilder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class DiagramCreator {

    private final NodeFactory nodeFactory;
    private final RelationshipFactory relationshipFactory;
    private final GraphDatabaseService graphDatabaseService;
    private final int depthLimit;

    public DiagramCreator(NodeFactory nodeFactory, RelationshipFactory relationshipFactory, GraphDatabaseService graphDatabaseService, int depthLimit) {
        this.nodeFactory = nodeFactory;
        this.relationshipFactory = relationshipFactory;
        this.graphDatabaseService = graphDatabaseService;
        this.depthLimit = depthLimit;
    }

    public void create(String fileName, String startPoint) throws IOException {
        try (Transaction tx = graphDatabaseService.beginTx()) {

            Node startNode = graphDatabaseService.findNode(TransportGraphBuilder.Labels.STATION,
                    GraphStaticKeys.ID, startPoint);
            List<String> seen = new LinkedList<>();

            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");

            visit(startNode, builder, seen, 0);

            builder.append("}");
            FileWriter writer = new FileWriter(fileName);
            writer.write(builder.toString());
            writer.close();

            tx.success();
        }
    }

    private void visit(Node rawNode, StringBuilder builder, List<String> seen, int depth) {
        if (depth>=depthLimit) {
            return;
        }

        TramNode startNode = nodeFactory.getNode(rawNode);
        final String startNodeId = startNode.getId().replace(" ","");

        if (seen.contains(startNodeId)) {
            return;
        }
        seen.add(startNodeId);


        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", startNodeId,
                getLabelFor(startNode), getShapeFor(startNode)));

        List<TramGoesToRelationship> services = new LinkedList<>();
        rawNode.getRelationships(Direction.OUTGOING).forEach(relationship -> {
            TransportRelationship tramRelat = relationshipFactory.getRelationship(relationship);

            Node rawEndNode = relationship.getEndNode();
            TramNode endNode = nodeFactory.getNode(rawEndNode);
            String endNodeId = endNode.getId().replace(" ", "");

            addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", endNodeId,
                    getLabelFor(endNode), getShapeFor(endNode)));

            // add links
            if (tramRelat.isGoesTo()) {
                TramGoesToRelationship serviceRelationship = (TramGoesToRelationship) tramRelat;
                if (!services.contains(serviceRelationship)) {
                    services.add(serviceRelationship);
                }
            } else if (tramRelat.isEnterPlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "E"));
            } else if (tramRelat.isLeavePlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "L"));
            } else if (tramRelat.isServiceLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "Svc"));
            } else if (tramRelat.isHourLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "H"));
            } else if (tramRelat.isMinuteLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "T"));
            }
            else {
                // boarding and depart
                String edgeLabel = tramRelat.isInterchange() ? "X" : "";
                if (tramRelat.isBoarding()) {
                    edgeLabel += "B";
                } else if (tramRelat.isDepartTram()) {
                    edgeLabel += "D";
                }
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, edgeLabel));
            }
            visit(rawEndNode, builder, seen, depth+1);
        });

        // add services for this node
        for (TramGoesToRelationship service : services) {
            String id;
            if (service.hasTripId()) {
                id = service.getTripId();
            } else {
                id = service.getServiceId();
            }
            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                    startNodeId, service.getEndNode().getId(), id
                    ));
        }

    }

    private String getShapeFor(TramNode node) {
        if (node.isPlatform()) {
            return "box";
        }
        if (node.isRouteStation()) {
            return "oval";
        }
        if (node.isStation()) {
            return "octagon";
        }
        if (node.isService()) {
            return "cds";
        }
        if (node.isHour()) {
            return "box";
        }
        if (node.isMinute()) {
            return "box";
        }
        return "box";
    }

    private String getLabelFor(TramNode node) {
        if (node.isPlatform()) {
            return node.getName();
        }
        if (node.isRouteStation()) {
            BoardPointNode bpNode = (BoardPointNode) node;
            return format("%s\n%s", bpNode.getRouteId(), bpNode.getName());
        }
        if (node.isStation()) {
            return node.getName();
        }
        if (node.isService()) {
            ServiceNode svcNode = (ServiceNode) node;
            return svcNode.getServiceId();
        }
        if (node.isHour()) {
            return node.getName();
        }
        if (node.isMinute()) {
            return node.getName();
        }
        return "";
    }

    private void addLine(StringBuilder builder, String line) {
        if (builder.lastIndexOf(line) < 0) {
            builder.append(line);
        }
    }
}
