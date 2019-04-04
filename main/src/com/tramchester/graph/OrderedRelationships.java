package com.tramchester.graph;

import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.TRIPS;
import static com.tramchester.graph.GraphStaticKeys.TRIP_ID;
import static com.tramchester.graph.TransportRelationshipTypes.*;

public class OrderedRelationships implements Iterable<Relationship> {
    private final TransportGraphBuilder.Labels nodeLabel;
    private final ServiceHeuristics serviceHeuristics;
    private final String inboundTripId;
    private final boolean inboundWasBoarding;
    private final LinkedList<Relationship> towardsServices;
    private final List<Relationship> towardsStation;

    public OrderedRelationships(TransportGraphBuilder.Labels nodeLabel, boolean inboundWasBoarding,
                                Relationship inbound, ServiceHeuristics serviceHeuristics) {
        this.nodeLabel = nodeLabel;
        this.serviceHeuristics = serviceHeuristics;
        if (nodeLabel==TransportGraphBuilder.Labels.ROUTE_STATION && !inboundWasBoarding) {
            inboundTripId = inbound.getProperty(TRIP_ID).toString();
        } else {
            inboundTripId = "";
        }
        towardsServices = new LinkedList<>();
        towardsStation = new ArrayList<>();

        this.inboundWasBoarding = inboundWasBoarding;
    }

    public void insert(Relationship relationship) {
        if (nodeLabel==TransportGraphBuilder.Labels.PLATFORM) {
            insertForPlatform(relationship);
        } else {
            insertForRS(relationship);
        }
    }

    private void insertForPlatform(Relationship outbound) {
        TransportRelationshipTypes type = TransportRelationshipTypes.valueOf(outbound.getType().name());

        if (type==LEAVE_PLATFORM) {
            towardsStation.add(outbound);
        } else if (serviceHeuristics.preferedRoute(outbound)) {
            towardsServices.addFirst(outbound);
        } else {
            towardsServices.addLast(outbound);
        }

    }

    public void insertForRS(Relationship outbound) {

        TransportRelationshipTypes type = TransportRelationshipTypes.valueOf(outbound.getType().name());

        // just boarded
        if (inboundWasBoarding) {
            if (serviceHeuristics.preferedRoute(outbound)) {
                towardsServices.addFirst(outbound);
            } else {
                towardsServices.addLast(outbound);
            }
            return;
        }

        // departing
        boolean departing = type==DEPART || type==INTERCHANGE_DEPART;
        if (departing) {
            towardsStation.add(outbound);
            return;
        }

        // towardsServices (TO_SERVICE)
        String outboundTrips = outbound.getProperty(TRIPS).toString();
        if (outboundTrips.contains(inboundTripId)) {
            towardsServices.addFirst(outbound);
        } else {
            towardsServices.addLast(outbound);
        }
    }

    public boolean isEmpty() {
        return towardsServices.isEmpty();
    }

    @Override
    public Iterator<Relationship> iterator() {
        towardsStation.forEach(depart->{
            if (serviceHeuristics.toEndStation(depart)) {
                towardsServices.addFirst(depart);
            } else {
                towardsServices.addLast(depart);
            }
            });
        return towardsServices.iterator();
    }

    public long size() {
        return towardsServices.size();
    }
}
