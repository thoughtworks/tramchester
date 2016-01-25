package com.tramchester.graph.Relationships;

import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TramGoesToRelationship extends TramCostRelationship {
    private String service;
    private boolean[] daysRunning;
    private int[] timesRunning;
    private String dest;
    private TransportMode mode;
    private TramServiceDate startDate;
    private TramServiceDate endDate;

    public TramGoesToRelationship(String service, int cost, boolean[] daysRunning, int[] timesRunning, String id,
                                  TramServiceDate startDate, TramServiceDate endDate, String dest, TransportMode mode) {
        //TESTING ONLY
        super(cost, id);
        this.service = service;
        this.daysRunning = daysRunning;
        this.timesRunning = timesRunning;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dest = dest;
        this.mode = mode;
    }

    public TramGoesToRelationship(Relationship graphRelationship) {
        super(graphRelationship);
        mode = TransportMode.Tram;
    }

    @Override
    public TransportMode getMode() {
        return mode;
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
            dest = graphRelationship.getProperty(GraphStaticKeys.ROUTE_STATION).toString();
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

    @Override
    public boolean isWalk() {
        return false;
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
