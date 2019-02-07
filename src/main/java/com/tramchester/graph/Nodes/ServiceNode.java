package com.tramchester.graph.Nodes;

import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Relationships.RelationshipFactory;
import com.tramchester.graph.Relationships.ServiceRelationship;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.LinkedList;
import java.util.List;

import static org.neo4j.graphdb.Direction.OUTGOING;

public class ServiceNode implements TramNode {
    private final String id;
    private final String name;
    private final Node node;
    private boolean[] daysRunning;

    public ServiceNode(Node node) {
        this.node = node;
        this.name = node.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
    }

    @Override
    public boolean isStation() {
        return false;
    }

    @Override
    public boolean isRouteStation() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isQuery() {
        return false;
    }

    @Override
    public boolean isService() {
        return true;
    }

    @Override
    public boolean isHour() {
        return false;
    }

    @Override
    public boolean isMinute() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isPlatform() {
        return false;
    }

    public boolean[] getDaysServiceRuns() {
        if (daysRunning==null) {
            daysRunning = (boolean[]) node.getProperty(GraphStaticKeys.DAYS);
        }
        return daysRunning;
    }

    public List<TramGoesToRelationship> getOutbound(RelationshipFactory factory) {
        List<TramGoesToRelationship> list = new LinkedList<>();
        Iterable<Relationship> outgoingRelationships = node.getRelationships(OUTGOING, TransportRelationshipTypes.TRAM_GOES_TO);
        outgoingRelationships.forEach(outgoing -> list.add((TramGoesToRelationship) factory.getRelationship(outgoing)));
        return list;
    }
}
