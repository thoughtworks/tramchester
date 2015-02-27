package com.tramchester.graph;

import com.google.common.collect.Lists;
import com.tramchester.domain.DaysOfWeek;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.ArrayList;
import java.util.List;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class TimeBasedPathExpander implements PathExpander<GraphBranchState> {
    private CostEvaluator<Double> costEvaluator;

    public TimeBasedPathExpander(CostEvaluator<Double> costEvaluator) {
        this.costEvaluator = costEvaluator;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<GraphBranchState> state) {
        List<Relationship> results = new ArrayList<>();

        double currentWeight = new WeightedPathImpl(costEvaluator, path).weight();
        int currentTime = (int) (currentWeight + state.getState().getTime());

        for (Relationship relationship : getRelationships(path)) {

            if (relationship.isType(TransportRelationshipTypes.GOES_TO)) {
                String service_id = relationship.getProperty("service_id").toString();
                boolean[] days = (boolean[]) relationship.getProperty(DAYS);
                int[] times = (int[]) relationship.getProperty(TIMES);
                if (operatesOnDay(days, state.getState().getDay()) && operatesOnTime(times, currentTime)) {
                    results.add(relationship);
                }
            } else {
                results.add(relationship);
            }
        }

        return results;
    }

    private List<Relationship> getRelationships(Path path) {
        List<Relationship> relationships = Lists.newArrayList(path.endNode().getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.GOES_TO.name())));
        if (isStation(path)) {
            relationships.addAll(Lists.newArrayList(path.endNode().getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.BOARD.name()))));
        } else {
            relationships.addAll(Lists.newArrayList(path.endNode().getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.DEPART.name()))));
        }
        return relationships;
    }

    private boolean isStation(Path path) {
        return path.endNode().hasProperty(NAME);
    }

    private boolean operatesOnTime(int[] times, int currentTime) {
        for (int i = 0; i < times.length - 1; i++) {
            if (currentTime >= times[i] && currentTime <= times[i + 1]) {
                return true;
            }
        }
        return false;
    }

    private boolean operatesOnDay(boolean[] days, DaysOfWeek today) {
        switch (today) {
            case Monday:
                return days[0];
            case Tuesday:
                return days[1];
            case Wednesday:
                return days[2];
            case Thursday:
                return days[3];
            case Friday:
                return days[4];
            case Saturday:
                return days[5];
            case Sunday:
                return days[6];
        }
        return false;
    }

    @Override
    public PathExpander<GraphBranchState> reverse() {
        return this;
    }
}

