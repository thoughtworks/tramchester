package com.tramchester.graph.search;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.caches.NodeContentsRepository;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.graphbuild.GraphProps;
import com.tramchester.graph.search.stateMachine.HowIGotHere;
import com.tramchester.repository.CompositeStationRepository;
import org.apache.commons.lang3.tuple.Pair;
import org.neo4j.graphdb.*;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@LazySingleton
public class ReasonsToGraphViz {

    private final CompositeStationRepository stationRepository;
    private final NodeContentsRepository nodeContentsRepository;

    private static final boolean includeAll = true;

    @Inject
    public ReasonsToGraphViz(CompositeStationRepository stationRepository, NodeContentsRepository nodeContentsRepository) {
        this.stationRepository = stationRepository;
        this.nodeContentsRepository = nodeContentsRepository;
    }

    private void add(ServiceReason reason, Transaction transaction, StringBuilder builder, State state) {
        HowIGotHere howIGotHere = reason.getHowIGotHere();

        long endNodeId = howIGotHere.getEndNodeId();
        String reasonId = reason.getReasonCode().name() + endNodeId;
        Node currentNode = transaction.getNodeById(endNodeId);

        addNodeToDiagram(currentNode, builder, state);

        if (includeAll || !reason.isValid()) {
            if (!state.reasonIds.contains(reasonId)) {
                state.reasonIds.add(reasonId);
                String shape = reason.isValid() ? "oval" : "octagon";
                builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", reasonId, reason.textForGraph(), shape));
            }

            Pair<Long, String> reasonLink = Pair.of(endNodeId, reasonId);
            if (!state.reasonRelationships.contains(reasonLink)) {
                state.reasonRelationships.add(reasonLink);
                builder.append(format("\"%s\"->\"%s\"", endNodeId, reasonId));
            }
        }

        if (!howIGotHere.atStart()) {
            Relationship relationship = transaction.getRelationshipById(howIGotHere.getRelationshipId());
            Node fromNode = relationship.getStartNode();
            addNodeToDiagram(fromNode, builder, state);

            long fromNodeId = fromNode.getId();
            Pair<Long,Long> link = Pair.of(fromNodeId, endNodeId);
            if (!state.relationships.contains(link)) {
                state.relationships.add(link);
                RelationshipType relationshipType = relationship.getType();
                builder.append(format("\"%s\"->\"%s\" [label=\"%s\"]", fromNodeId, endNodeId, relationshipType.name()));
            }
        }
    }

    private void addNodeToDiagram(Node node, StringBuilder builder, State state) {
        long nodeId = node.getId();
        if (!state.nodes.contains(nodeId)) {
            state.nodes.add(nodeId);
            StringBuilder nodeLabel = new StringBuilder();
            nodeContentsRepository.getLabels(node).forEach(label -> nodeLabel.append(label.name()).append(" "));
            String id = getIdsFor(node);
            nodeLabel.append("\n").append(id);
            builder.append(format("\"%s\" [label=\"%s\"] [shape=%s];\n", nodeId, nodeLabel, "hexagon"));
        }
    }

    private String getIdsFor(Node node) {
        StringBuilder ids = new StringBuilder();
        EnumSet<GraphLabel> labels = nodeContentsRepository.getLabels(node);

        if (labels.contains(GraphLabel.STATION)) {
            IdFor<Station> stationIdFrom = GraphProps.getStationIdFrom(node);
            Station station = stationRepository.getStationById(stationIdFrom);
            ids.append(System.lineSeparator()).append(station.getName());
        } else if (labels.contains(GraphLabel.INTERCHANGE)) {
            String value = GraphProps.getRouteIdFrom(node).forDTO();
            if (ids.length() > 0) {
                ids.append(System.lineSeparator());
            }
            ids.append(value);
        }

        return ids.toString();
    }

    public void appendTo(StringBuilder builder, List<ServiceReason> reasons, Transaction txn) {
        State state = new State();
        reasons.forEach(reason -> add(reason, txn, builder, state));
        state.clear();
    }

    private static class State {
        private final Set<Long> nodes;
        private final Set<String> reasonIds;
        private final Set<Pair<Long,Long>> relationships;
        private final Set<Pair<Long, String>> reasonRelationships;

        private State() {
            nodes = new HashSet<>();
            reasonIds = new HashSet<>();
            relationships = new HashSet<>();
            reasonRelationships = new HashSet<>();
        }

        public void clear() {
            nodes.clear();
            reasonIds.clear();
            relationships.clear();
            reasonRelationships.clear();
        }
    }
}
