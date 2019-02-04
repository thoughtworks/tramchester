package com.tramchester.graph;

import com.tramchester.domain.TramServiceDate;
import org.neo4j.graphdb.Node;

import static com.tramchester.graph.GraphStaticKeys.HOUR;

public class CachedNodeOperations implements NodeOperations {

    @Override
    public boolean[] getDays(Node node) {
        return (boolean[]) node.getProperty(GraphStaticKeys.DAYS);
    }

    @Override
    public TramServiceDate getServiceStartDate(Node node) {
        return new TramServiceDate(node.getProperty(GraphStaticKeys.SERVICE_START_DATE).toString());
    }

    @Override
    public TramServiceDate getServiceEndDate(Node node) {
        return new TramServiceDate(node.getProperty(GraphStaticKeys.SERVICE_END_DATE).toString());
    }

    @Override
    public int getHour(Node node) {
        return (int) node.getProperty(HOUR);
    }

    @Override
    public boolean isService(Node node) {
        return node.hasLabel(TransportGraphBuilder.Labels.SERVICE);
    }

    @Override
    public boolean isHour(Node node) {
        return node.hasLabel(TransportGraphBuilder.Labels.HOUR);
    }
}
