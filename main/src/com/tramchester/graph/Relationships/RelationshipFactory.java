package com.tramchester.graph.Relationships;

import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Relationship;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RelationshipFactory {

    private ConcurrentMap<Long,TransportRelationship> cache;

    private NodeFactory nodeFactory;

    public RelationshipFactory(NodeFactory nodeFactory) {
        cache = new ConcurrentHashMap<>();
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
        try {
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
                case TO_SERVICE: return new ServiceRelationship(graphRelationship, nodeFactory);
                case TO_HOUR: return new HourRelationship(graphRelationship, nodeFactory);
                case TO_MINUTE: return new MinuteRelationship(graphRelationship,nodeFactory);
                case ON_ROUTE: return new RouteRelationship(graphRelationship, nodeFactory);
                default:
                    throw new IllegalArgumentException("Unexpected relationship type: " + relationshipType);
            }
        }
        catch(java.lang.IllegalArgumentException unexpected) {
            throw new RuntimeException("Unable to find match for "+ name, unexpected);
        }

    }
}
