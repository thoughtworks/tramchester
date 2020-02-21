package com.tramchester;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.TransportGraphBuilder;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static com.tramchester.graph.GraphStaticKeys.TRIP_ID;
import static java.lang.String.format;


// TODO Rewrite to use query instead
public class DiagramCreator {

    private final GraphDatabaseService graphDatabaseService;
    private final int depthLimit;

    public DiagramCreator(GraphDatabaseService graphDatabaseService, int depthLimit) {
        this.graphDatabaseService = graphDatabaseService;
        this.depthLimit = depthLimit;
    }

    public void create(String filename, String startPoint) throws IOException {
        create(filename, Collections.singletonList(startPoint));
    }

    public void create(String fileName, List<String> startPointsList) throws IOException {

        List<Long> seen = new LinkedList<>();
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

    private void visit(Node node, StringBuilder builder, List<Long> seen, int depth) {
        if (depth>=depthLimit) {
            return;
        }
        if (seen.contains(node.getId())) {
            return;
        }
        seen.add(node.getId());

        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", createNodeId(node),
                getLabelFor(node), getShapeFor(node)));

        visitOutbounds(node, builder, seen, depth);
        visitInbounds(node, builder, seen, depth);

    }

    private void visitInbounds(Node targetNode, StringBuilder builder, List<Long> seen, int depth) {
        targetNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.values()).forEach(towards -> {

            Node startNode = towards.getStartNode();
            addNode(builder, startNode);

            // startNode -> targetNode
            addEdge(builder, towards, createNodeId(startNode), createNodeId(targetNode));
            visit(startNode, builder, seen, depth+1);
        });
    }

    private void visitOutbounds(Node startNode, StringBuilder builder, List<Long> seen, int depth) {
        Map<Long,Relationship> tramGoesToRelationships = new HashMap<>();

        startNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.forPlanning()).forEach(awayFrom -> {

            TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(awayFrom.getType().name());

            Node rawEndNode = awayFrom.getEndNode();

            addNode(builder, startNode);
            addEdge(builder, awayFrom, createNodeId(startNode), createNodeId(rawEndNode));

            if (relationshipType==TransportRelationshipTypes.TRAM_GOES_TO) {
                if (!tramGoesToRelationships.containsKey(awayFrom.getId())) {
                    tramGoesToRelationships.put(awayFrom.getId(), awayFrom);
                }
            }
            visit(rawEndNode, builder, seen, depth+1);
        });

        // add services for this node
        // Node -> Service End Node
        for (Relationship tramGoesToRelationship : tramGoesToRelationships.values()) {
            String id;
            if (tramGoesToRelationship.hasProperty(TRIP_ID)) {
                id = tramGoesToRelationship.getProperty(TRIP_ID).toString();
            } else {
                id = tramGoesToRelationship.getProperty(SERVICE_ID).toString();
            }
            if (id.isEmpty()) {
                id = "NO_ID";
            }
            builder.append(format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                    createNodeId(startNode), createNodeId(tramGoesToRelationship.getEndNode()), id));
        }
    }


    private void addEdge(StringBuilder builder, Relationship edge, String startNodeId, String endNodeId) {

        TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(edge.getType().name());

        if (relationshipType==TransportRelationshipTypes.TRAM_GOES_TO) {
            // use outbound
        } else if (relationshipType==TransportRelationshipTypes.ON_ROUTE) {
            String routeId = edge.getProperty(GraphStaticKeys.ROUTE_ID).toString();
            addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "ROUTE:"+routeId));
        } else {
            String shortForm = createShortForm(relationshipType);
            addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, shortForm));
        }
    }

    private void addNode(StringBuilder builder, Node sourceNode) {
        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", createNodeId(sourceNode),
                getLabelFor(sourceNode), getShapeFor(sourceNode)));
    }

    private String createNodeId(Node sourceNode) {
        return String.valueOf(sourceNode.getId());
    }

    private String getShapeFor(Node node) {
        if (node.hasLabel(TransportGraphBuilder.Labels.PLATFORM)) {
            return "box";
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)) {
            return "oval";
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.STATION)) {
            return "house";
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.SERVICE)) {
            return "octagon";
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.HOUR)) {
            return "box";
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.MINUTE)) {
            return "circle";
        }

        return "box";
    }

    private String getLabelFor(Node node) {
        if (node.hasLabel(TransportGraphBuilder.Labels.PLATFORM)) {
            return getNameOfNode(node);
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.ROUTE_STATION)) {
            // TODO Look up station name from the ID?
            String stationName = node.getProperty(GraphStaticKeys.STATION_ID).toString();
            return format("%s\n%s", node.getProperty(GraphStaticKeys.ROUTE_ID).toString(), stationName);
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.STATION)) {
            return getNameOfNode(node);
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.SERVICE)) {
            return node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.HOUR)) {
            return node.getProperty(GraphStaticKeys.HOUR).toString();
        }
        if (node.hasLabel(TransportGraphBuilder.Labels.MINUTE)) {
            String fullTime = node.getProperty(GraphStaticKeys.TIME).toString();
            int index = fullTime.indexOf(":");
            return fullTime.substring(index);
        }

        return "No_Label";
    }

    private String getNameOfNode(Node node) {
        // todo look up station name from ID?
        return node.getProperty(GraphStaticKeys.ID).toString();
    }

    private void addLine(StringBuilder builder, String line) {
        if (builder.lastIndexOf(line) < 0) {
            builder.append(line);
        }
    }

    private String createShortForm(TransportRelationshipTypes relationshipType) {
        switch (relationshipType) {
            case ENTER_PLATFORM: return "E";
            case LEAVE_PLATFORM: return "L";
            case WALKS_TO: return "W";
            case BOARD: return "B";
            case TO_SERVICE: return "Svc";
            case TO_HOUR: return "H";
            case TO_MINUTE: return "T";
            case ON_ROUTE: return "R";
            case INTERCHANGE_BOARD: return "IB";
            case INTERCHANGE_DEPART: return "ID";
            case TRAM_GOES_TO: return "Tram";
            case DEPART: return "D";
            case BUS_GOES_TO: return "Bus";
            default: return "Unkn";
        }
    }
}
