package com.tramchester.graph.graphbuild;

import com.tramchester.domain.places.Station;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.graph.GraphDatabase;
import com.tramchester.graph.NodeTypeRepository;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.tramchester.graph.TransportRelationshipTypes.NEIGHBOUR;
import static com.tramchester.graph.graphbuild.GraphProps.setProperty;
import static java.lang.String.format;

public class CreateNodesAndRelationships {
    private static final Logger logger = LoggerFactory.getLogger(CreateNodesAndRelationships.class);

    protected final GraphDatabase graphDatabase;
    protected final NodeTypeRepository nodeTypeRepository;

    private int numberNodes;
    private int numberRelationships;

    public CreateNodesAndRelationships(GraphDatabase graphDatabase, NodeTypeRepository nodeTypeRepository) {
        this.graphDatabase = graphDatabase;
        this.nodeTypeRepository = nodeTypeRepository;
        numberNodes = 0;
        numberRelationships = 0;
    }

    protected Node createStationNode(Transaction tx, Station station) {

        Set<GraphBuilder.Labels> labels = GraphBuilder.Labels.forMode(station.getTransportModes());
        logger.debug(format("Creating station node: %s with labels: %s ", station, labels));
        Node stationNode = createGraphNode(tx, labels);
        setProperty(stationNode, station);
        return stationNode;
    }

    protected Node createGraphNode(Transaction tx, GraphBuilder.Labels label) {
        numberNodes++;
        Node node = graphDatabase.createNode(tx, label);
        nodeTypeRepository.put(node.getId(), label);
        return node;
    }

    private Node createGraphNode(Transaction tx, Set<GraphBuilder.Labels> labels) {
        numberNodes++;
        Node node = graphDatabase.createNode(tx, labels);
        nodeTypeRepository.put(node.getId(), labels);
        return node;
    }

    protected Relationship createRelationship(Node start, Node end, TransportRelationshipTypes relationshipType) {
        numberRelationships++;
        return start.createRelationshipTo(end, relationshipType);
    }

    protected void reportStats() {
        logger.info("Nodes created: " + numberNodes);
        logger.info("Relationships created: " + numberRelationships);
    }

    protected boolean addNeighbourRelationship(Node fromNode, Node toNode, int cost) {

        Set<Long> alreadyNeighbours = new HashSet<>();
        fromNode.getRelationships(Direction.OUTGOING, NEIGHBOUR).forEach(relationship -> alreadyNeighbours.add(relationship.getEndNode().getId()));

        if (!alreadyNeighbours.contains(toNode.getId())) {
            Relationship relationship = createRelationship(fromNode, toNode, NEIGHBOUR);
            GraphProps.setCostProp(relationship, cost);
            return true;
        }
        return false;
    }
}
