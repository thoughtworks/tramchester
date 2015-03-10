package com.tramchester.graph;

import com.google.common.collect.Lists;
import com.tramchester.domain.DaysOfWeek;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.impl.util.WeightedPathImpl;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.BranchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tramchester.graph.GraphStaticKeys.*;
import static com.tramchester.graph.GraphStaticKeys.Station.NAME;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class TimeBasedPathExpander implements PathExpander<GraphBranchState> {
    private static final Logger logger = LoggerFactory.getLogger(TimeBasedPathExpander.class);
    private static final int MAX_WAIT_TIME = 15; // todo into config
    private CostEvaluator<Double> costEvaluator;

    public TimeBasedPathExpander(CostEvaluator<Double> costEvaluator) {
        this.costEvaluator = costEvaluator;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<GraphBranchState> state) {
        List<Relationship> results = new ArrayList<>();

        int duration = (int)new WeightedPathImpl(costEvaluator, path).weight();
        int journeyStartTime = state.getState().getTime();
        int elapsedTime = duration + journeyStartTime;

        Set<Relationship> relationships = getRelationships(path);

        for (Relationship relationship : relationships) {

            if (relationship.isType(TransportRelationshipTypes.GOES_TO)) {
                boolean[] days = (boolean[]) relationship.getProperty(DAYS);
                int[] times = (int[]) relationship.getProperty(TIMES);
                if (operatesOnDay(days, state.getState().getDay())
                        && operatesOnTime(times, elapsedTime)) {
                    results.add(relationship);
                }
            } else {
                results.add(relationship);
            }
        }
        if (duration>90) {
            logger.warn("Duration >90mins at node " + path.endNode().getProperty("id"));
        }
        return results;
    }

    private Set<Relationship> getRelationships(Path path) {
        Node endNode = path.endNode();
        HashSet<Relationship> relationships = new HashSet<>();

        List<Relationship> goesTo = Lists.newArrayList(endNode.getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.GOES_TO.name())));
        relationships.addAll(goesTo);
        if (isStation(path)) {
            if (path.length()==0) {
                logger.debug("Add boarding relationships");
                relationships.addAll(Lists.newArrayList(endNode.getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.BOARD.name()))));
            }
            ArrayList<Relationship> interchanges = Lists.newArrayList(endNode.getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.INTERCHANGE.name())));
            relationships.addAll(interchanges);
        } else {
            relationships.addAll(Lists.newArrayList(endNode.getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.DEPART.name()))));
        }

        return relationships;
    }

    private boolean isStation(Path path) {
        return path.endNode().hasProperty(NAME);
    }

    private boolean operatesOnTime(int[] times, int currentTime) {
        // the times array is sorted in ascending order
        for (int i = 0; i < times.length - 1; i++) {
            int timeA = times[i];
            int timeB = times[i + 1];
            if ((timeB-currentTime)>MAX_WAIT_TIME) {
                // next tram too far in future, so stop searching now
                return false;
            }

            if (currentTime >= timeA && currentTime <= timeB)   {
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

