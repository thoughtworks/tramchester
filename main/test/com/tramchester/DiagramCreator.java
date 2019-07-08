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
import java.time.LocalTime;
import java.util.*;

import static java.lang.String.format;

public class DiagramCreator {

    private final NodeFactory nodeFactory;
    private final RelationshipFactory relationshipFactory;
    private final GraphDatabaseService graphDatabaseService;
    private final int depthLimit;

    public DiagramCreator(NodeFactory nodeFactory, RelationshipFactory relationshipFactory, GraphDatabaseService graphDatabaseService,
                          int depthLimit) {
        this.nodeFactory = nodeFactory;
        this.relationshipFactory = relationshipFactory;
        this.graphDatabaseService = graphDatabaseService;
        this.depthLimit = depthLimit;
    }

    public void create(String filename, String startPoint) throws IOException {
        create(filename, Collections.singletonList(startPoint));
    }

    public void create(String fileName, List<String> startPointsList) throws IOException {

        List<String> seen = new LinkedList<>();
        StringBuilder builder = new StringBuilder();

        try (Transaction tx = graphDatabaseService.beginTx()) {
            builder.append("digraph G {\n");

            startPointsList.forEach(startPoint -> {
                Node startNode = graphDatabaseService.findNode(TransportGraphBuilder.Labels.STATION,
                        GraphStaticKeys.ID, startPoint);
                visit(startNode, builder, seen, 0);
            });

            builder.append("}");
            tx.success();
        }

        FileWriter writer = new FileWriter(fileName);
        writer.write(builder.toString());
        writer.close();
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

        visitOutbounds(rawNode, builder, seen, depth, currentNodeId);
        visitInbounds(rawNode, builder, seen, depth, currentNodeId);

    }

    private void visitInbounds(Node rawNode, StringBuilder builder, List<String> seen, int depth, String currentNodeId) {
        rawNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.values()).forEach(inboundEdge -> {
            TransportRelationship inboundRelationship = relationshipFactory.getRelationship(inboundEdge);

            Node rawSourceNode = inboundEdge.getStartNode();
            TramNode sourceNode = nodeFactory.getNode(rawSourceNode);
            String sourceNodeId = createNodeId(sourceNode);

            addNode(builder, sourceNode, sourceNodeId);

            if (inboundRelationship.isGoesTo()) {
                // use outbound for this
            } else if (inboundRelationship.isEnterPlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "E"));
            }
            else if (inboundRelationship.isLeavePlatform()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "L"));
            } else if (inboundRelationship.isServiceLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "Svc"));
            } else if (inboundRelationship.isHourLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "H"));
            } else if (inboundRelationship.isMinuteLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "T"));
            } else if (inboundRelationship.isEndServiceLink()) {
                addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", sourceNodeId, currentNodeId, "End"));
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
    }

    private void addNode(StringBuilder builder, TramNode sourceNode, String sourceNodeId) {
        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", sourceNodeId,
                getLabelFor(sourceNode), getShapeFor(sourceNode)));
    }

    private void visitOutbounds(Node rawNode, StringBuilder builder, List<String> seen, int depth, String currentNodeId) {
        Set<TramGoesToRelationship> tramGoesToRelationships = new HashSet<>();

        rawNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.values()).forEach(outputBoundEdge -> {
            TransportRelationship outboundRelationship = relationshipFactory.getRelationship(outputBoundEdge);

            Node rawEndNode = outputBoundEdge.getEndNode();
            TramNode destinationNode = nodeFactory.getNode(rawEndNode);
            String endNodeId = createNodeId(destinationNode);

            addNode(builder, destinationNode, endNodeId);

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
            return "circle";
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
            String fullTime = node.getName();
            int index = fullTime.indexOf(":");
            return fullTime.substring(index);
        }
        if (node.isServiceEnd()) {
            ServiceEndNode serviceEndNode = (ServiceEndNode) node;
            return serviceEndNode.getId();
        }
        return "No_Label";
    }

    private void addLine(StringBuilder builder, String line) {
        if (builder.lastIndexOf(line) < 0) {
            builder.append(line);
        }
    }
}
