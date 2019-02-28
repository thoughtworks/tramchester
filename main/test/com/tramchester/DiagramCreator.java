package com.tramchester;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.*;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

        TramNode currentNode = nodeFactory.getNode(rawNode);
        final String currentNodeId = createNodeId(currentNode);

        if (seen.contains(currentNodeId)) {
            return;
        }
        seen.add(currentNodeId);

        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", currentNodeId,
                getLabelFor(currentNode), getShapeFor(currentNode)));

        Set<TramGoesToRelationship> tramGoesToRelationships = new HashSet<>();

        rawNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.values()).forEach(outputBoundEdge -> {
            TransportRelationship outboundRelationship = relationshipFactory.getRelationship(outputBoundEdge);

            Node rawEndNode = outputBoundEdge.getEndNode();
            TramNode destinationNode = nodeFactory.getNode(rawEndNode);
            String endNodeId = createNodeId(destinationNode);

            addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", endNodeId,
                    getLabelFor(destinationNode), getShapeFor(destinationNode)));

            // add links
            if (outboundRelationship.isGoesTo()) {
                TramGoesToRelationship serviceRelationship = (TramGoesToRelationship) outboundRelationship;
                if (!tramGoesToRelationships.contains(serviceRelationship)) {
                    tramGoesToRelationships.add(serviceRelationship);
                }
            } else if (outboundRelationship.isEnterPlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", currentNodeId, endNodeId, "E"));
            } else if (outboundRelationship.isLeavePlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", currentNodeId, endNodeId, "L"));
            } else if (outboundRelationship.isServiceLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", currentNodeId, endNodeId, "Svc"));
            } else if (outboundRelationship.isHourLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", currentNodeId, endNodeId, "H"));
            } else if (outboundRelationship.isMinuteLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", currentNodeId, endNodeId, "T"));
            }
            else {
                // boarding and depart
                String edgeLabel = outboundRelationship.isInterchange() ? "X" : "";
                if (outboundRelationship.isBoarding()) {
                    edgeLabel += "B";
                } else if (outboundRelationship.isDepartTram()) {
                    edgeLabel += "D";
                }
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", currentNodeId, endNodeId, edgeLabel));
            }
            visit(rawEndNode, builder, seen, depth+1);
        });

        rawNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.values()).forEach(inboundEdge -> {
            TransportRelationship inboundRelationship = relationshipFactory.getRelationship(inboundEdge);

            Node rawSourceNode = inboundEdge.getStartNode();
            TramNode sourceNode = nodeFactory.getNode(rawSourceNode);
            String sourceNodeId = createNodeId(sourceNode);

            addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", sourceNodeId,
                    getLabelFor(sourceNode), getShapeFor(sourceNode)));

            if (inboundRelationship.isGoesTo()) {
                // nop
            } else if (inboundRelationship.isEnterPlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "E"));
            } else if (inboundRelationship.isLeavePlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "L"));
            } else if (inboundRelationship.isServiceLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "Svc"));
            } else if (inboundRelationship.isHourLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "H"));
            } else if (inboundRelationship.isMinuteLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "T"));
            }
            else {
                // boarding and depart
                String edgeLabel = inboundRelationship.isInterchange() ? "X" : "";
                if (inboundRelationship.isBoarding()) {
                    edgeLabel += "B";
                } else if (inboundRelationship.isDepartTram()) {
                    edgeLabel += "D";
                }
                if (edgeLabel.isEmpty()) {
                    edgeLabel = "??";
                }
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, edgeLabel));
            }
            visit(rawSourceNode, builder, seen, depth+1);
        });

        // add services for this node
        // Node -> Service End Node
        for (TramGoesToRelationship tramGoesToRelationship : tramGoesToRelationships) {
            String id;
            if (tramGoesToRelationship.hasTripId()) {
                id = tramGoesToRelationship.getTripId();
            } else {
                id = tramGoesToRelationship.getServiceId();
            }
            if (id.isEmpty()) {
                id = "NO_ID";
            }
            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                    currentNodeId, createNodeId(tramGoesToRelationship.getEndNode()), id));
        }

    }

    private String createNodeId(TramNode sourceNode) {
        return sourceNode.getId().replace(" ", "");
    }

    private String getShapeFor(TramNode node) {
        if (node.isPlatform()) {
            return "box";
        }
        if (node.isRouteStation()) {
            return "oval";
        }
        if (node.isStation()) {
            return "house";
        }
        if (node.isService()) {
            return "octagon";
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
        return "No_Label";
    }

    private void addLine(StringBuilder builder, String line) {
        if (builder.lastIndexOf(line) < 0) {
            builder.append(line);
        }
    }
}
