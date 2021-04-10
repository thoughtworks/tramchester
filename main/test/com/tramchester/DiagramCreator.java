package com.tramchester;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.GraphQuery;
import com.tramchester.graph.TransportRelationshipTypes;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;
import java.util.*;

import static com.tramchester.graph.GraphPropertyKey.*;
import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.HOUR;
import static com.tramchester.graph.graphbuild.GraphBuilder.Labels.*;
import static java.lang.String.format;


@LazySingleton
public class DiagramCreator {
    private static final Logger logger = LoggerFactory.getLogger(DiagramCreator.class);

    private final GraphDatabase graphDatabase;
    private final GraphQuery graphQuery;

    @Inject
    public DiagramCreator(GraphDatabase graphDatabase, GraphQuery graphQuery) {
        // ready is token to express dependency on having a built graph DB
        this.graphDatabase = graphDatabase;
        this.graphQuery = graphQuery;
    }

    public void create(String filename, Station station, int depthLimit) throws IOException {
        create(filename, Collections.singleton(station), depthLimit);
    }

    // TODO Path not String  for fileName
    public void create(String fileName, Collection<Station> startPointsList, int depthLimit) throws IOException {
        logger.info("Creating diagram " + fileName);

        Set<Long> nodeSeen = new HashSet<>();
        Set<Long> relationshipSeen = new HashSet<>();

        OutputStream fileStream = new FileOutputStream(fileName);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileStream);
        PrintStream printStream = new PrintStream(bufferedOutputStream);

        DiagramBuild builder = new DiagramBuild(printStream);

        try (Transaction txn = graphDatabase.beginTx()) {
            builder.append("digraph G {\n");

            startPointsList.forEach(startPoint -> {

                Node startNode = graphQuery.getStationNode(txn, startPoint);

                if (startNode==null) {
                    logger.error("Can't find start node for station " + startPoint.getId());
                    builder.append("MISSING NODE\n");
                } else {
                    visit(startNode, builder, depthLimit, nodeSeen, relationshipSeen);
                }
            });

            builder.append("}");
        }

        relationshipSeen.clear();
        nodeSeen.clear();

        printStream.close();
        bufferedOutputStream.close();
        fileStream.close();

        logger.info("Finished diagram");
    }

    private void visit(Node node, DiagramBuild builder, int depth, Set<Long> nodeSeen, Set<Long> relationshipSeen) {
        if (depth<=0) {
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
            visit(startNode, builder, depth-1, nodeSeen, relationshipSeen);
        });
    }

    private void visitOutbounds(Node startNode, DiagramBuild builder, int depth, Set<Long> seen, Set<Long> relationshipSeen) {
        Map<Long,Relationship> goesToRelationships = new HashMap<>();

        startNode.getRelationships(Direction.OUTGOING, TransportRelationshipTypes.forPlanning()).forEach(awayFrom -> {

            TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(awayFrom.getType().name());

            Node rawEndNode = awayFrom.getEndNode();

            addNode(builder, startNode);
            addEdge(builder, awayFrom, createNodeId(startNode), createNodeId(rawEndNode), relationshipSeen);

            if (relationshipType==TRAM_GOES_TO || relationshipType==BUS_GOES_TO || relationshipType==TRAIN_GOES_TO) {
                if (!goesToRelationships.containsKey(awayFrom.getId())) {
                    goesToRelationships.put(awayFrom.getId(), awayFrom);
                }
            }
            visit(rawEndNode, builder, depth-1, seen, relationshipSeen);
        });

        // add services for this node
        // Node -> Service End Node
        for (Relationship tramGoesToRelationship : goesToRelationships.values()) {
            String id;
            if (GraphProps.hasProperty(TRIP_ID, tramGoesToRelationship)) {
                id = GraphProps.getTripId(tramGoesToRelationship).getGraphId();
            } else {
                id = GraphProps.getServiceId(tramGoesToRelationship).getGraphId();
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

        if (relationshipType==TransportRelationshipTypes.ON_ROUTE) {
            String routeId = GraphProps.getRouteId(edge).getGraphId();
            addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "R:"+routeId));
        } else if (relationshipType== LINKED) {
            Set<TransportMode> modes = GraphProps.getTransportModes(edge);
            addLine(builder, format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, "L:"+modes));
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
        if (node.hasLabel(TRAM_STATION) || node.hasLabel(BUS_STATION) || node.hasLabel(TRAIN_STATION)) {
            return "house";
        }
        if (node.hasLabel(GraphBuilder.Labels.SERVICE)) {
            return "octagon";
        }
        if (node.hasLabel(HOUR)) {
            return "box";
        }
        if (node.hasLabel(GraphBuilder.Labels.MINUTE)) {
            return "box";
        }

        return "box";
    }

    private String getLabelFor(Node node) {
        if (node.hasLabel(PLATFORM)) {
            return node.getProperty(PLATFORM_ID.getText()).toString();
        }
        if (node.hasLabel(ROUTE_STATION)) {
            // TODO Look up station name from the ID?
            String stationName = GraphProps.getStationId(node).getGraphId();
            TransportMode mode = GraphProps.getTransportMode(node);
            String graphId = GraphProps.getRouteId(node).getGraphId();
            return format("%s\n%s\n%s", graphId, stationName, mode.name());
        }
        if (node.hasLabel(TRAM_STATION) || node.hasLabel(BUS_STATION) || node.hasLabel(TRAIN_STATION)) {
            return node.getProperty(STATION_ID.getText()).toString();
        }
        if (node.hasLabel(SERVICE)) {
            return GraphProps.getServiceId(node).getGraphId();
        }
        if (node.hasLabel(HOUR)) {
            return GraphProps.getHour(node).toString();
        }
        if (node.hasLabel(MINUTE)) {
            return GraphProps.getTime(node).toString();
        }

        return "No_Label";
    }


    private void addLine(DiagramBuild builder, String line) {
        builder.append(line);
    }

    private String createShortForm(TransportRelationshipTypes relationshipType) {
        return switch (relationshipType) {
            case ENTER_PLATFORM -> "E";
            case LEAVE_PLATFORM -> "L";
            case WALKS_TO -> "W";
            case BOARD -> "B";
            case TO_SERVICE -> "Svc";
            case TO_HOUR -> "H";
            case TO_MINUTE -> "T";
            case ON_ROUTE -> "R";
            case INTERCHANGE_BOARD -> "IB";
            case INTERCHANGE_DEPART -> "ID";
            case TRAM_GOES_TO -> "Tram";
            case DEPART -> "D";
            case BUS_GOES_TO -> "Bus";
            case TRAIN_GOES_TO -> "Train";
            case LINKED -> "Link";
            case NEIGHBOUR -> "neigh";
            case FERRY_GOES_TO -> "Ferry";
            case SUBWAY_GOES_TO -> "Subway";
            default -> "Unkn";
        };
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
