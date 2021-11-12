package com.tramchester.graph.caches;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Service;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.places.RouteStation;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphLabel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.EnumSet;

@ImplementedBy(CachedNodeOperations.class)
public interface NodeContentsRepository  {

    IdFor<RouteStation> getRouteStationId(Node node);
    IdFor<Service> getServiceId(Node node);

    TramTime getTime(Node node);
    int getHour(Node node);

    IdFor<Trip> getTrip(Relationship relationship);
    int getCost(Relationship lastRelationship);
    void deleteFromCostCache(Relationship relationship);

    EnumSet<GraphLabel> getLabels(Node node);
}
