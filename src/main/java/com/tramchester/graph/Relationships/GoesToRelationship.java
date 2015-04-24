package com.tramchester.graph.Relationships;

import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;

public class GoesToRelationship extends TramCostRelationship {
    private String service;
    private boolean[] daysRunning;
    private int[] timesRunning;

    public GoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning) {
        super(cost);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
    }

    public GoesToRelationship(Relationship graphRelationship) {
        super(graphRelationship);

        daysRunning = (boolean[]) graphRelationship.getProperty(GraphStaticKeys.DAYS);
        timesRunning = (int[]) graphRelationship.getProperty(GraphStaticKeys.TIMES);
        service = graphRelationship.getProperty(GraphStaticKeys.SERVICE_ID).toString();
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

    @Override
    public String toString() {
        return "GoesToRelationship{" +
                "service='" + service + '\'' +
                ", daysRunning=" + Arrays.toString(daysRunning) +
                ", timesRunning=" + Arrays.toString(timesRunning) +
                ", cost=" + super.getCost() +
                '}';
    }


}
