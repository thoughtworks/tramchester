package com.tramchester.graph.Nodes;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class QueryNode implements TramNode {

    private final LatLong latLong;

    public QueryNode(Node node) {
        double lat = Double.parseDouble(node.getProperty(GraphStaticKeys.Station.LAT).toString());
        double lon = Double.parseDouble(node.getProperty(GraphStaticKeys.Station.LONG).toString());
        this.latLong = new LatLong(lat,lon);
    }

    @Override
    public boolean isStation() {
        return false;
    }

    @Override
    public boolean isRouteStation() {
        return false;
    }

    @Override
    public String getId() {
        return "Query";
    }

    @Override
    public boolean isQuery() {
        return true;
    }

    @Override
    public String getName() {
        return "Your location";
    }

    @Override
    public boolean isPlatform() {
        return false;
    }

    public LatLong getLatLon() {
        return latLong;
    }
}
