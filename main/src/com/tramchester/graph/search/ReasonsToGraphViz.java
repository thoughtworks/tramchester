package com.tramchester.graph.search;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphBuilder;
import com.tramchester.graph.search.states.HowIGotHere;
import com.tramchester.repository.TransportData;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.HashSet;

import static java.lang.String.format;

public class ReasonsToGraphViz {

    private final StringBuilder builder;
    private final Transaction transaction;
    private final TransportData transportData;

    private final HashSet<Long> nodes;
    private final HashSet<String> reasonIds;
    private final HashSet<Pair<Long,Long>> relationships;
    private final HashSet<Pair<Long, String>> reasonRelationships;

    private final boolean includeAll = false;

    public ReasonsToGraphViz(Transaction transaction, TransportData transportData, StringBuilder builder) {
        this.transportData = transportData;
        this.builder = builder;
        this.transaction = transaction;

        nodes = new HashSet<>();
        reasonIds = new HashSet<>();
        relationships = new HashSet<>();
        reasonRelationships = new HashSet<>();
    }

    public void add(ServiceReason reason) {
        HowIGotHere howIGotHere = reason.getHowIGotHere();

        long endNodeId = howIGotHere.getEndNodeId();
        String reasonId = reason.getReasonCode().name() + endNodeId;
        Node currentNode = transaction.getNodeById(endNodeId);

        addNodeToDiagram(currentNode);

        if (includeAll || !reason.isValid()) {
            if (!reasonIds.contains(reasonId)) {
                reasonIds.add(reasonId);
                String shape = reason.isValid() ? "oval" : "octagon";
                builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", reasonId, reason.textForGraph(), shape));
            }

            Pair<Long, String> reasonLink = Pair.of(endNodeId, reasonId);
            if (!reasonRelationships.contains(reasonLink)) {
                reasonRelationships.add(reasonLink);
                builder.append(format("\"%s\"->\"%s\"", endNodeId, reasonId));
            }
        }

        if (!howIGotHere.atStart()) {
            Relationship relationship = transaction.getRelationshipById(howIGotHere.getRelationshipId());
            Node fromNode = relationship.getStartNode();
            addNodeToDiagram(fromNode);

            long fromNodeId = fromNode.getId();
            Pair<Long,Long> link = Pair.of(fromNodeId, endNodeId);
            if (!relationships.contains(link)) {
                relationships.add(link);
                RelationshipType relationshipType = relationship.getType();
                builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", fromNodeId, endNodeId, relationshipType.name()));
            }
        }
    }

    private void addNodeToDiagram(Node node) {
        long nodeId = node.getId();
        if (!nodes.contains(nodeId)) {
            nodes.add(nodeId);
            StringBuilder nodeLabel = new StringBuilder();
            node.getLabels().forEach(label -> nodeLabel.append(label.name()).append(" "));
            String id = getIdsFor(node);
            nodeLabel.append("\n").append(id);
            builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", nodeId, nodeLabel, "hexagon"));
        }
    }

    private String getIdsFor(Node node) {
        StringBuilder ids = new StringBuilder();
        node.getLabels().forEach(label -> {
            GraphBuilder.Labels graphLabel = GraphBuilder.Labels.valueOf(label.name());
            GraphPropertyKey key = GraphPropertyKey.keyForLabel(graphLabel);
            String value = node.getProperty(key.getText()).toString();
            if (ids.length()>0) {
                ids.append(System.lineSeparator());
            }
            ids.append(value);
            if (GraphBuilder.Labels.isStation(graphLabel)) {
                Station station = transportData.getStationById(IdFor.getStationIdFrom(node));
                ids.append(System.lineSeparator()).append(station.getName());
            }
        });
        return ids.toString();
    }

}
