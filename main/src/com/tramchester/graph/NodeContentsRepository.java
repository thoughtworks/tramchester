package com.tramchester.graph;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Service;
import com.tramchester.domain.input.Trip;
import com.tramchester.domain.time.TramTime;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

@ImplementedBy(CachedNodeOperations.class)
public interface NodeContentsRepository  {

    IdFor<Service> getServiceId(Node node);
    TramTime getTime(Node node);
    int getHour(Node node);

    IdFor<Trip> getTrip(Relationship relationship);
    String getTrips(Relationship relationship);

    int getCost(Relationship lastRelationship);
    void deleteFromCostCache(Relationship relationship);
}
