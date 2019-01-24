package com.tramchester.graph.Relationships;

import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.Map;

public class RelationshipFactory {

    private Map<Long,TransportRelationship> cache;

    private NodeFactory nodeFactory;

    public RelationshipFactory(NodeFactory nodeFactory) {
        cache = new HashMap<>();
        this.nodeFactory = nodeFactory;
    }

    public TransportRelationship getRelationship(Relationship graphRelationship) {
        long id = graphRelationship.getId();
        if (cache.containsKey(id)) {
            return cache.get(id);
        }

        TransportRelationship result = createRelationship(graphRelationship);
        cache.put(id,result);
        return result;
    }

    private TransportRelationship createRelationship(Relationship graphRelationship) {
        String name = graphRelationship.getType().name();
        TransportRelationshipTypes relationshipType = TransportRelationshipTypes.valueOf(name);
        switch (relationshipType) {
            case TRAM_GOES_TO: return new TramGoesToRelationship(graphRelationship, nodeFactory);
            case BUS_GOES_TO: return new BusGoesToRelationship(graphRelationship, nodeFactory);
            case DEPART: return new DepartRelationship(graphRelationship, nodeFactory);
            case BOARD: return new BoardRelationship(graphRelationship, nodeFactory);
            case INTERCHANGE_DEPART: return new InterchangeDepartsRelationship(graphRelationship, nodeFactory);
            case INTERCHANGE_BOARD: return new InterchangeBoardsRelationship(graphRelationship, nodeFactory);
            case WALKS_TO: return new WalksToRelationship(graphRelationship, nodeFactory);
            case ENTER_PLATFORM: return new EnterPlatformRelationship(graphRelationship, nodeFactory);
            case LEAVE_PLATFORM: return new LeavePlatformRelationship(graphRelationship, nodeFactory);
            // edge per service
            case SERVICE: return new ServiceRelationship(graphRelationship, nodeFactory);
            default:
                throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
        }
    }
}
