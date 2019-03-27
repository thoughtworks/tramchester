package com.tramchester.graph.Nodes;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.GraphStaticKeys;
import org.neo4j.graphdb.Node;

public class QueryNode extends TramNode {

    private final LatLong latLong;

    public QueryNode(Node node) {
        double lat = Double.parseDouble(node.getProperty(GraphStaticKeys.Station.LAT).toString());
        double lon = Double.parseDouble(node.getProperty(GraphStaticKeys.Station.LONG).toString());
        this.latLong = new LatLong(lat,lon);
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

    public LatLong getLatLon() {
        return latLong;
    }
}
