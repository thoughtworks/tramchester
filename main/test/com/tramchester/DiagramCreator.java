package com.tramchester;

import com.tramchester.graph.*;
import com.tramchester.graph.graphbuild.GraphBuilder;
import org.neo4j.graphdb.*;

import java.io.*;
import java.util.*;

import static com.tramchester.graph.GraphStaticKeys.SERVICE_ID;
import static com.tramchester.graph.GraphStaticKeys.TRIP_ID;
import static java.lang.String.format;


// TODO Rewrite to use query instead
public class DiagramCreator {

    private final GraphDatabase graphDatabaseService;
    private final int depthLimit;

    public DiagramCreator(GraphDatabase graphDatabaseService, int depthLimit) {
        this.graphDatabaseService = graphDatabaseService;
        this.depthLimit = depthLimit;
    }

    public void create(String filename, String startPoint) throws IOException {
        create(filename, Collections.singletonList(startPoint));
    }

    public void create(String fileName, List<String> startPointsList) throws IOException {

        Set<Long> nodeSeen = new HashSet<>();
        Set<Long> relationshipSeen = new HashSet<>();

        OutputStream fileStream = new FileOutputStream(fileName);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        DiagramBuild builder = new DiagramBuild(printStream);

        try (Transaction tx = graphDatabaseService.beginTx()) {
            builder.append("digraph G {\n");

            startPointsList.forEach(startPoint -> {
                Node startNode = graphDatabaseService.findNode(tx, GraphBuilder.Labels.TRAM_STATION,
                        GraphStaticKeys.ID, startPoint);
                visit(startNode, builder, 0, nodeSeen, relationshipSeen);
            });

            builder.append("}");
        }

        relationshipSeen.clear();
        nodeSeen.clear();

        printStream.close();
        bufferedOutputStream.close();
        fileStream.close();
    }

    private void visit(Node node, DiagramBuild builder, int depth, Set<Long> nodeSeen, Set<Long> relationshipSeen) {
        if (depth>=depthLimit) {
            return;
        }
        if (nodeSeen.contains(node.getId())) {
            return;
        }
        nodeSeen.add(node.getId());

        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", createNodeId(node),
                getLabelFor(node), getShapeFor(node)));

        visitOutbounds(node, builder, depth, nodeSeen, relationshipSeen);
        visitInbounds(node, builder, depth, nodeSeen, relationshipSeen);

    }

    private void visitInbounds(Node targetNode, DiagramBuild builder, int depth, Set<Long> nodeSeen, Set<Long> relationshipSeen) {
        targetNode.getRelationships(Direction.INCOMING, TransportRelationshipTypes.values()).forEach(towards -> {

            Node startNode = towards.getStartNode();
            addNode(builder, startNode);

            // startNode -> targetNode
            addEdge(builder, towards, createNodeId(startNode), createNodeId(targetNode), relationshipSeen);
            visit(startNode, builder, depth+1, nodeSeen, relationshipSeen);
        });
    }

    private void visitOutbounds(Node startNode, DiagramBuild builder, int depth, Set<Long> seen, Set<Long> relationshipSeen) {
        Map<Long,Relationship> tramGoesToRelationships = new HashMap<>();

        startNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.forPlanning()).forEach(awayFrom -> {

            TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(awayFrom.getType().name());

            Node rawEndNode = awayFrom.getEndNode();

            addNode(builder, startNode);
            addEdge(builder, awayFrom, createNodeId(startNode), createNodeId(rawEndNode), relationshipSeen);

            if (relationshipType==TransportRelationshipTypes.TRAM_GOES_TO) {
                if (!tramGoesToRelationships.containsKey(awayFrom.getId())) {
                    tramGoesToRelationships.put(awayFrom.getId(), awayFrom);
                }
            }
            visit(rawEndNode, builder, depth+1, seen, relationshipSeen);
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


    private void addEdge(DiagramBuild builder, Relationship edge, String startNodeId, String endNodeId, Set<Long> relationshipSeen) {

        TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(edge.getType().name());

        if (relationshipSeen.contains(edge.getId())) {
            return;
        }
        relationshipSeen.add(edge.getId());

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

    private void addNode(DiagramBuild builder, Node sourceNode) {
        addLine(builder, format("\"%s\" [label=\"%s\" shape=%s];\n", createNodeId(sourceNode),
                getLabelFor(sourceNode), getShapeFor(sourceNode)));
    }

    private String createNodeId(Node sourceNode) {
        return String.valueOf(sourceNode.getId());
    }

    private String getShapeFor(Node node) {
        if (node.hasLabel(GraphBuilder.Labels.PLATFORM)) {
            return "box";
        }
        if (node.hasLabel(GraphBuilder.Labels.ROUTE_STATION)) {
            return "oval";
        }
        if (node.hasLabel(GraphBuilder.Labels.TRAM_STATION)) {
            return "house";
        }
        if (node.hasLabel(GraphBuilder.Labels.SERVICE)) {
            return "octagon";
        }
        if (node.hasLabel(GraphBuilder.Labels.HOUR)) {
            return "box";
        }
        if (node.hasLabel(GraphBuilder.Labels.MINUTE)) {
            return "circle";
        }

        return "box";
    }

    private String getLabelFor(Node node) {
        if (node.hasLabel(GraphBuilder.Labels.PLATFORM)) {
            return getNameOfNode(node);
        }
        if (node.hasLabel(GraphBuilder.Labels.ROUTE_STATION)) {
            // TODO Look up station name from the ID?
            String stationName = node.getProperty(GraphStaticKeys.STATION_ID).toString();
            return format("%s\n%s", node.getProperty(GraphStaticKeys.ROUTE_ID).toString(), stationName);
        }
        if (node.hasLabel(GraphBuilder.Labels.TRAM_STATION)) {
            return getNameOfNode(node);
        }
        if (node.hasLabel(GraphBuilder.Labels.SERVICE)) {
            return node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        }
        if (node.hasLabel(GraphBuilder.Labels.HOUR)) {
            return node.getProperty(GraphStaticKeys.HOUR).toString();
        }
        if (node.hasLabel(GraphBuilder.Labels.MINUTE)) {
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

    private void addLine(DiagramBuild builder, String line) {
        builder.append(line);
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

    private static class DiagramBuild {
        private final PrintStream printStream;

        public DiagramBuild(PrintStream printStream) {

            this.printStream = printStream;
        }

        public void append(String text) {
            printStream.print(text);
        }
    }
}
