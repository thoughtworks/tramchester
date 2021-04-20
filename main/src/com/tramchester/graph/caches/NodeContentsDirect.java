package com.tramchester.graph.caches;

import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphProps;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

// KEEP for assisting with debugging
public class NodeContentsDirect implements NodeContentsRepository {

    @Override
    public IdFor<RouteStation> getRouteStationId(Node node) {
        return GraphProps.getRouteStationIdFrom(node);
    }

    @Override
    public IdFor<Service> getServiceId(Node node) {
        return GraphProps.getServiceIdFrom(node);
    }

    @Override
    public TramTime getTime(Node node) {
        return GraphProps.getTime(node);
    }

    @Override
    public int getHour(Node node) {
        return GraphProps.getHour(node);
    }

    @Override
    public IdFor<Trip> getTrip(Relationship relationship) {
        return GraphProps.getTripId(relationship);
    }

    @Override
    public int getCost(Relationship relationship) {
        return GraphProps.getCost(relationship);
    }

    @Override
    public void deleteFromCostCache(Relationship relationship) {
        // no-op
    }
}
