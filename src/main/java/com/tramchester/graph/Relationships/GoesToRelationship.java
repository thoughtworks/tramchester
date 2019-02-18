package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

import java.time.LocalTime;

public abstract class GoesToRelationship extends TransportCostRelationship {
    private String service;
    private boolean[] daysRunning;
    private TramServiceDate startDate;
    private TramServiceDate endDate;
    private String tripId;

    // TODO array to disappear
    private LocalTime[] timesRunning; // OR if edge per trip then
    private LocalTime timeRunning;

    protected GoesToRelationship(String service, int cost, boolean[] daysRunning, LocalTime[] timesRunning, String id,
                                 TramServiceDate startDate, TramServiceDate endDate,
                                 TramNode startNode, TramNode endNode, String tripId) {
        //TESTING ONLY
        super(cost, id, startNode, endNode);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public LocalTime[] getTimesServiceRuns() {
        if (timesRunning==null) {
            timesRunning = (LocalTime[]) graphRelationship.getProperty(GraphStaticKeys.TIMES);
        }
        return timesRunning;
    }

    @Deprecated
    public LocalTime getTimeServiceRuns() {
        if (timeRunning==null) {
            timeRunning =(LocalTime) graphRelationship.getProperty(GraphStaticKeys.DEPART_TIME);
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

    public TramServiceDate getStartDate() {
        if (startDate==null) {
            startDate = new TramServiceDate(graphRelationship.getProperty(GraphStaticKeys.SERVICE_START_DATE).toString());
        }
        return startDate;
    }

    public TramServiceDate getEndDate() {
        if (endDate==null) {
            endDate = new TramServiceDate(graphRelationship.getProperty(GraphStaticKeys.SERVICE_END_DATE).toString());
        }
        return endDate;
    }

    @Override
    public boolean isGoesTo() {
        return true;
    }

    @Override
    public String toString() {
        return "TramGoesToRelationship{" +
                "service='" + getServiceId() + '\'' +
                ", startDate=" + getStartDate() +
                ", endDate=" + getEndDate() +
                '}';
    }

}
