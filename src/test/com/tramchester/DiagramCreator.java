package com.tramchester;

import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
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

    private NodeFactory nodeFactory;
    private RelationshipFactory relationshipFactory;
    private GraphDatabaseService graphDatabaseService;

    public DiagramCreator(NodeFactory nodeFactory, RelationshipFactory relationshipFactory, GraphDatabaseService graphDatabaseService) {
        this.nodeFactory = nodeFactory;
        this.relationshipFactory = relationshipFactory;
        this.graphDatabaseService = graphDatabaseService;
    }

    public void create(String fileName, String startPoint) throws IOException, TramchesterException {
        try (Transaction tx = graphDatabaseService.beginTx()) {

            Node startNode = graphDatabaseService.findNode(TransportGraphBuilder.Labels.STATION,
                    GraphStaticKeys.ID, startPoint);
            List<String> seen = new LinkedList<>();

            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");

            visit(startNode, builder, seen);

            builder.append("}");
            FileWriter writer = new FileWriter(fileName);
            writer.write(builder.toString());
            writer.close();

            tx.success();
        }
    }

    private void visit(Node rawNode, StringBuilder builder, List<String> seen) throws TramchesterException {
        TramNode startNode = nodeFactory.getNode(rawNode);
        final String startNodeId = startNode.getId().replace(" ","");

        if (seen.contains(startNodeId)) {
            return;
        }
        seen.add(startNodeId);

        List<String> services = new LinkedList<>();

        rawNode.getRelationships(Direction.OUTGOING).forEach(relationship -> {
            TransportRelationship tramRelat = relationshipFactory.getRelationship(relationship);
            Node rawEndNode = relationship.getEndNode();
            try {
                TramNode endNode = nodeFactory.getNode(rawEndNode);
                String endNodeId = endNode.getId().replace(" ", "");
                // add node
                String prefix = selectPrefixFor(startNode);
                addLine(builder, format("\"%s\" [label=\"%s%s\n%s\"];\n", startNodeId,
                        prefix, startNode.getName(), startNodeId));
                // add links
                if (tramRelat.isGoesTo()) {
                    if (!services.contains(endNodeId)) {
                        services.add(endNodeId);
                    }
                } else if (tramRelat.isEnterPlatform()) {
                    addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "E"));
                } else if (tramRelat.isLeavePlatform()) {
                    addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "L"));
                } else {
                    // boarding and depart
                    String label = tramRelat.isInterchange() ? "X" : "";
                    if (tramRelat.isBoarding()) {
                        label += "B";
                    } else if (tramRelat.isDepartTram()) {
                        label += "D";
                    }
                    addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, label));
                }
                visit(rawEndNode, builder, seen);
            }
            catch (TramchesterException exception) {
                System.console().writer().println("Unable to visit node " + exception);
            }
        });

        // add services for this node
        for (String endNodeId : services) {
            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                    startNodeId, endNodeId,
                    "svc"));
        }

    }

    private String selectPrefixFor(TramNode endNode) {
        if (endNode.isPlatform()) {
            return "P:";
        }
        if (endNode.isRouteStation()) {
            return "BP:";
        }
        if (endNode.isStation()) {
            return "S:";
        }
        return "";
    }

    private void addLine(StringBuilder builder, String line) {
        if (builder.lastIndexOf(line) < 0) {
            builder.append(line);
        }
    }
}
