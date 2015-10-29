package com.tramchester.graph;


import com.tramchester.Dependencies;
import com.tramchester.IntegrationTestConfig;
import com.tramchester.Stations;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.TramRelationship;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.ID;

public class CreateDotDiagramTest {
    private static Dependencies dependencies;
    private GraphDatabaseService graphService;
    private RelationshipFactory relationshipFactory;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws Exception {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTestConfig());
    }

    @Before
    public void beforeEachOfTheTestsRun() {
        graphService = dependencies.get(GraphDatabaseService.class);
        relationshipFactory = new RelationshipFactory();
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldProduceADotDiagramOfTheTramNetwork() throws IOException {
        try (Transaction tx = graphService.beginTx()) {

            Node startNode = graphService.findNode(DynamicLabel.label(TransportGraphBuilder.STATION),
                    "id", Stations.VeloPark);
            List<Node> seen = new LinkedList<>();

            StringBuilder builder = new StringBuilder();
            builder.append("digraph G {");

            visit(startNode, builder, seen);

            builder.append("}");
            FileWriter writer = new FileWriter("diagram.dot");
            writer.write(builder.toString());
            writer.close();

            tx.success();
        }

    }

    private void visit(Node node, StringBuilder builder, List<Node> seen) {
        if (seen.contains(node)) {
            return;
        } else {
            seen.add(node);
        }
        List<String> services = new LinkedList<>();
        String startNodeId = node.getProperty(ID).toString();

        node.getRelationships(Direction.OUTGOING).forEach(relationship -> {
            TramRelationship tramRelat = relationshipFactory.getRelationship(relationship);
            Node endNode = relationship.getEndNode();
            String endNodeId = endNode.getProperty(ID).toString();

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
                builder.append(String.format("\"%s\"->\"%s\" [label=\"%s\"];\n", startNodeId, endNodeId, label));
            }
            visit(endNode, builder, seen);
        });
        for (String endNodeId : services) {
            builder.append(String.format("\"%s\"->\"%s\" [label=\"%s\"];\n",
                    startNodeId, endNodeId,
                    "svc"));
        }

    }

}
