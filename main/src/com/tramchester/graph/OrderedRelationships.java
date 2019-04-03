package com.tramchester.graph;

import org.neo4j.graphdb.Relationship;

import java.util.Iterator;
import java.util.LinkedList;

import static com.tramchester.graph.GraphStaticKeys.TRIPS;
import static com.tramchester.graph.GraphStaticKeys.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class OrderedRelationships implements Iterable<Relationship> {
    private final ServiceHeuristics serviceHeuristics;
    private final String inboundTripId;
    private final boolean inboundWasBoarding;
    LinkedList<Relationship> theList;

    public OrderedRelationships(boolean inboundWasBoarding, Relationship inbound, ServiceHeuristics serviceHeuristics) {
        this.serviceHeuristics = serviceHeuristics;
        inboundTripId = inboundWasBoarding ? "" : inbound.getProperty(TRIP_ID).toString();
        theList = new LinkedList<>();
        this.inboundWasBoarding = inboundWasBoarding;
    }

    public void insert(Relationship outbound) {
        if (inboundWasBoarding) {
            if (serviceHeuristics.preferedRoute(outbound)) {
                theList.addFirst(outbound);
            } else {
                theList.addLast(outbound);
            }
            return;
        }
        boolean departing = outbound.isType(DEPART) || outbound.isType(INTERCHANGE_DEPART);
        if (departing) {
            theList.addFirst(outbound);
            return;
        }

        boolean isGoesTo = outbound.isType(TRAM_GOES_TO);
        String outboundTrips;
        if (isGoesTo) {
            outboundTrips = outbound.getProperty(TRIP_ID).toString();
        } else {
            outboundTrips = outbound.getProperty(TRIPS).toString();
        }
        if (outboundTrips.contains(inboundTripId)) {
            theList.addFirst(outbound);
        } else {
            theList.addLast(outbound);
        }
    }

    public boolean isEmpty() {
        return theList.isEmpty();
    }

    @Override
    public Iterator<Relationship> iterator() {
        return theList.iterator();
    }

    public long size() {
        return theList.size();
    }
}
