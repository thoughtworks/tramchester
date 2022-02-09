package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.*;
import static com.tramchester.graph.graphbuild.GraphProps.setProperty;
import static java.lang.String.format;

public class CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(CreateNodesAndRelationships.class);

    protected final GraphDatabase graphDatabase;

    private int numberNodes;
    private int numberRelationships;

    public CreateNodesAndRelationships(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
        numberNodes = 0;
        numberRelationships = 0;
    }

    protected Node createStationNode(Transaction tx, Station station) {

        Set<GraphLabel> labels = GraphLabel.forMode(station.getTransportModes());
        labels.add(GraphLabel.STATION);
        if (station.hasPlatforms()) {
            labels.add(GraphLabel.HAS_PLATFORMS);
        }
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        Node stationNode = createGraphNode(tx, labels);
        setProperty(stationNode, station);
        return stationNode;
    }

    protected Node createGraphNode(Transaction tx, GraphLabel label) {
        numberNodes++;
        return graphDatabase.createNode(tx, label);
    }

    private Node createGraphNode(Transaction tx, Set<GraphLabel> labels) {
        numberNodes++;
        return graphDatabase.createNode(tx, labels);
    }

    protected Relationship createRelationship(Node start, Node end, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected boolean addNeighbourRelationship(Node fromNode, Node toNode, Duration walkCost) {
        return addRelationshipFor(fromNode, toNode, walkCost, NEIGHBOUR);
    }

    protected void addGroupRelationshipTowardsParent(Node fromNode, Node toNode, Duration walkCost) {
        addRelationshipFor(fromNode, toNode, walkCost, GROUPED_TO_PARENT);
    }

    protected void addGroupRelationshipTowardsChild(Node fromNode, Node toNode, Duration walkCost) {
        addRelationshipFor(fromNode, toNode, walkCost, GROUPED_TO_CHILD);
    }

    private boolean addRelationshipFor(Node fromNode, Node toNode, Duration walkCost, TransportRelationshipTypes relationshipType) {
        Set<Long> alreadyRelationship = new HashSet<>();
        fromNode.getRelationships(Direction.OUTGOING, relationshipType).
                forEach(relationship -> alreadyRelationship.add(relationship.getEndNode().getId()));

        if (!alreadyRelationship.contains(toNode.getId())) {
            Relationship relationship = createRelationship(fromNode, toNode, relationshipType);
            GraphProps.setCostProp(relationship, walkCost);
            GraphProps.setMaxCostProp(relationship, walkCost);
            return true;
        }
        return false;
    }
}
