package com.tramchester.graph.Nodes;


import com.google.common.collect.Lists;
import com.tramchester.graph.GraphStaticKeys;
import com.tramchester.graph.TransportRelationshipTypes;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class StationNode implements TramNode {
    private final String id;
    private final String name;
    private final Node node;

    public StationNode(Node node) {
        this.node = node;
        this.id = node.getProperty(GraphStaticKeys.ID).toString();
        this.name = node.getProperty(GraphStaticKeys.Station.NAME).toString();
    }

    @Override
    public String toString() {
        return "StationNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean isStation() {
        return true;
    }

    @Override
    public boolean isRouteStation() {
        return false;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

}
