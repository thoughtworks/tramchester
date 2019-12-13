package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TramTime;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

public abstract class GoesToRelationship extends TransportCostRelationship {
    private String service;
    private boolean[] daysRunning; // TODO REMOVE
    private String tripId;

    // TODO array to disappear
    private TramTime[] timesRunning; // OR if edge per trip then
    private TramTime timeRunning;

    protected GoesToRelationship(String service, int cost, boolean[] daysRunning, TramTime[] timesRunning, String id,
                                 TramNode startNode, TramNode endNode, String tripId) {
        //TESTING ONLY
        super(cost, id, startNode, endNode);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
        this.tripId = tripId;
    }

    public GoesToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public boolean[] getDaysServiceRuns() {
        if (daysRunning==null) {
            daysRunning = (boolean[]) graphRelationship.getProperty(GraphStaticKeys.DAYS);
        }
        return daysRunning;
    }

    public TramTime[] getTimesServiceRuns() {
        if (timesRunning==null) {
            LocalTime[] rawTimesRunning = (LocalTime[]) graphRelationship.getProperty(GraphStaticKeys.TIMES);
            timesRunning = new TramTime[rawTimesRunning.length];
            for (int i = 0; i < rawTimesRunning.length; i++) {
                timesRunning[i] = TramTime.of(rawTimesRunning[i]);
            }
        }
        return timesRunning;
    }

    @Deprecated
    public TramTime getTimeServiceRuns() {
        if (timeRunning==null) {
            LocalTime property = (LocalTime) graphRelationship.getProperty(GraphStaticKeys.DEPART_TIME);
            timeRunning = TramTime.of(property);
        }
        return timeRunning;
    }

    public boolean hasTripId() {
        if (tripId!=null) {
            return !tripId.isEmpty();
        }
        return graphRelationship.hasProperty(GraphStaticKeys.TRIP_ID);
    }

    public String getTripId() {
        if (tripId==null) {
            tripId = (String) graphRelationship.getProperty(GraphStaticKeys.TRIP_ID);
        }
        return tripId;
    }

    public String getServiceId() {
        if (service==null) {
            service = graphRelationship.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        }
        return service;
    }

    @Override
    public boolean isGoesTo() {
        return true;
    }

    @Override
    public String toString() {
        return "TramGoesToRelationship{" +
                "service='" + getServiceId() + '\'' +
                '}';
    }

}
