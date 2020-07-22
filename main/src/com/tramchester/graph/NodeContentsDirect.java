package com.tramchester.graph;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

import static com.tramchester.graph.GraphStaticKeys.*;

// KEEP for assisting with debugging
public class NodeContentsDirect implements NodeContentsRepository{

    @Override
    public IdFor<Service> getServiceId(Node node) {
        return IdFor.getIdFrom(node,SERVICE_ID);
    }

    @Override
    public TramTime getTime(Node node) {
        return TramTime.of(((LocalTime) node.getProperty(TIME)));
    }

    @Override
    public int getHour(Node node) {
        return (int) node.getProperty(HOUR);
    }

    @Override
    public String getTrip(Relationship relationship) {
        return relationship.getProperty(TRIP_ID).toString();
    }

    @Override
    public String getTrips(Relationship relationship) {
        return relationship.getProperty(TRIPS).toString();
    }

    @Override
    public int getCost(Relationship relationship) {
        return (int) relationship.getProperty(COST);
    }

    @Override
    public void deleteFromCostCache(Relationship relationship) {
        // no-op
    }
}
