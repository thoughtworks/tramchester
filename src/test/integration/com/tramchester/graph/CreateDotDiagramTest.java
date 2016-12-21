package com.tramchester.graph;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TransportRelationship;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CreateDotDiagramTest {
    private static Dependencies dependencies;
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;
    private NodeFactory nodeFactory;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachOfTheTestsRun() {
        graphService = dependencies.get(GraphDatabaseService.class);
        relationshipFactory = dependencies.get(RelationshipFactory.class);
        nodeFactory = dependencies.get(NodeFactory.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        try (Transaction tx = graphService.beginTx()) {

            Node startNode = graphService.findNode(TransportGraphBuilder.Labels.STATION,
                    GraphStaticKeys.ID, Stations.VeloPark.getId());
            List<Node> seen = new LinkedList<>();

            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {\n");

            visit(startNode, builder, seen);

            builder.append("}");
            FileWriter writer = new FileWriter("diagram.dot");
            writer.write(builder.toString());
            writer.close();

            tx.success();
        }

    }

    private void visit(Node rawNode, StringBuilder builder, List<Node> seen) {
        if (seen.contains(rawNode)) {
            return;
        } else {
            seen.add(rawNode);
        }
        TramNode node = nodeFactory.getNode(rawNode);
        List<String> services = new LinkedList<>();
        //
        final String startNodeId = getName(rawNode, node);

        rawNode.getRelationships(Direction.OUTGOING).forEach(relationship -> {
            TransportRelationship tramRelat = relationshipFactory.getRelationship(relationship);
            Node rawEndNode = relationship.getEndNode();
            TramNode endNode = nodeFactory.getNode(rawEndNode);
            String endNodeId = getName(rawEndNode, endNode);
            if (tramRelat.isGoesTo()) {
                if (!services.contains(endNodeId)) {
                    services.add(endNodeId);
                }
            } else {
                String label = tramRelat.isInterchange()?"X":"";
                if (tramRelat.isBoarding()) {
                    label += "B";
                } else if (tramRelat.isDepartTram()) {
                    label += "D";
                }

                String line = String.format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, label);
                if (builder.lastIndexOf(line)<0) {
                    builder.append(line);
                }

            }
            visit(rawEndNode, builder, seen);
        });
        for (String endNodeId : services) {
            builder.append(String.format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                    startNodeId, endNodeId,
                    "svc"));
        }

    }

    private String getName(Node rawNode, TramNode node) {
        return node.isStation()?
                rawNode.getProperty(GraphStaticKeys.Station.NAME).toString()+" Station"
                :rawNode.getProperty(GraphStaticKeys.RouteStation.STATION_NAME).toString();
    }

}
