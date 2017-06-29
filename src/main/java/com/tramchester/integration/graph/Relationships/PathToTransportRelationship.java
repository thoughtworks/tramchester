package com.tramchester.integration.graph.Relationships;

import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PathToTransportRelationship {
    private static final Logger logger = LoggerFactory.getLogger(PathToTransportRelationship.class);
    private RelationshipFactory relationshipFactory;

    public PathToTransportRelationship(RelationshipFactory relationshipFactory) {
        this.relationshipFactory = relationshipFactory;
    }

    public List<TransportRelationship> mapPath(WeightedPath path) {
        logger.info("Mapping path to stages, weight is " + path.weight());

        Iterable<Relationship> relationships = path.relationships();
        List<TransportRelationship> transportRelationships = new ArrayList<>();

        relationships.forEach(relationship -> transportRelationships.add(relationshipFactory.getRelationship(relationship)));

        return transportRelationships;
    }
}
