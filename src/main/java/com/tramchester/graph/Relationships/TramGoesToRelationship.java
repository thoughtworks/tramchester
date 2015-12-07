package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;

public class TramGoesToRelationship extends TramCostRelationship {
    private String service;
    private boolean[] daysRunning;
    private int[] timesRunning;
    private String dest;
    private TramServiceDate startDate;
    private TramServiceDate endDate;

    public TramGoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id,
                                  TramServiceDate startDate, TramServiceDate endDate) {
        super(cost, id);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public TramGoesToRelationship(Relationship graphRelationship) {
        super(graphRelationship);

        daysRunning = (boolean[]) graphRelationship.getProperty(GraphStaticKeys.DAYS);
        timesRunning = (int[]) graphRelationship.getProperty(GraphStaticKeys.TIMES);
        service = graphRelationship.getProperty(GraphStaticKeys.SERVICE_ID).toString();
        dest = graphRelationship.getProperty(GraphStaticKeys.ROUTE_STATION).toString();
        startDate = new TramServiceDate(graphRelationship.getProperty(GraphStaticKeys.SERVICE_START_DATE).toString());
        endDate = new TramServiceDate(graphRelationship.getProperty(GraphStaticKeys.SERVICE_END_DATE).toString());
    }

    public boolean[] getDaysTramRuns() {
        return daysRunning;
    }

    public int[] getTimesTramRuns() {
        return timesRunning;
    }

    @Override
    public boolean isTramGoesTo() {
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
        return "TramGoesToRelationship{" +
                "service='" + service + '\'' +
                ", daysRunning=" + Arrays.toString(daysRunning) +
                ", timesRunning=" + Arrays.toString(timesRunning) +
                ", dest='" + dest + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }

    public TramServiceDate getStartDate() {
        return startDate;
    }

    public TramServiceDate getEndDate() {
        return endDate;
    }
}
