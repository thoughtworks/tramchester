package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.Nodes.NodeFactory;
import com.tramchester.graph.Nodes.TramNode;
import org.neo4j.graphdb.Relationship;

public abstract class GoesToRelationship extends TransportCostRelationship {
    private String service;
    private boolean[] daysRunning;
    private int[] timesRunning;
    private String dest;
    private TramServiceDate startDate;
    private TramServiceDate endDate;

    protected GoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id,
                              TramServiceDate startDate, TramServiceDate endDate, String dest,
                              TramNode startNode, TramNode endNode) {
        //TESTING ONLY
        super(cost, id, startNode, endNode);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dest = dest;
    }

    public GoesToRelationship(Relationship graphRelationship, NodeFactory nodeFactory) {
        super(graphRelationship, nodeFactory);
    }

    public boolean[] getDaysTramRuns() {
        if (daysRunning==null) {
            daysRunning = (boolean[]) graphRelationship.getProperty(GraphStaticKeys.DAYS);
        }
        return daysRunning;
    }

    public int[] getTimesTramRuns() {
        if (timesRunning==null) {
            timesRunning = (int[]) graphRelationship.getProperty(GraphStaticKeys.TIMES);
        }
        return timesRunning;
    }

    public String getService() {
        if (service==null) {
            service = graphRelationship.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        }
        return service;
    }

    public String getDest() {
        if (dest==null) {
            dest = graphRelationship.getProperty(GraphStaticKeys.DESTINATION).toString();
        }
        return dest; }

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
                "service='" + getService() + '\'' +
                ", dest='" + getDest() + '\'' +
                ", startDate=" + getStartDate() +
                ", endDate=" + getEndDate() +
                '}';
    }

}
