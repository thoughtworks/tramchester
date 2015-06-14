package com.tramchester.graph.Relationships;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;

public class GoesToRelationship extends TramCostRelationship {
    private String service;
    private boolean[] daysRunning;
    private int[] timesRunning;
    private String dest;

    public GoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id) {
        super(cost, id);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
    }

    public GoesToRelationship(Relationship graphRelationship) {
        super(graphRelationship);

        daysRunning = (boolean[]) graphRelationship.getProperty(GraphStaticKeys.DAYS);
        timesRunning = (int[]) graphRelationship.getProperty(GraphStaticKeys.TIMES);
        service = graphRelationship.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        dest = graphRelationship.getProperty(GraphStaticKeys.ROUTE_STATION).toString();
    }

    public boolean[] getDaysTramRuns() {
        return daysRunning;
    }

    public int[] getTimesTramRuns() {
        return timesRunning;
    }

    @Override
    public boolean isGoesTo() {
        return true;
    }

    @Override
    public boolean isBoarding() {
        return false;
    }

    @Override
    public boolean isDepartTram() {
        return false;
    }

    @Override
    public boolean isInterchange() {
        return false;
    }

    public String getService() {
        return service;
    }

    public String getDest() { return dest; }

    @Override
    public String toString() {
        return "GoesToRelationship{" +
                "service='" + service + '\'' +
                "dest='" + dest + '\'' +
                ", daysRunning=" + Arrays.toString(daysRunning) +
                ", timesRunning=" + Arrays.toString(timesRunning) +
                ", cost=" + super.getCost() +
                ", id=" + super.getId() +
                '}';
    }


}
