package com.tramchester.graph;


import com.google.common.collect.Lists;
import com.tramchester.domain.DaysOfWeek;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.BranchState;

import java.util.ArrayList;
import java.util.List;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;

public class TripPathExpander implements PathExpander<Integer> {

    private DaysOfWeek today;
    private int initialTime;

    public TripPathExpander(DaysOfWeek today, int initialTime) {
        this.today = today;
        this.initialTime = initialTime;
    }

    @Override
    public Iterable<Relationship> expand(Path path, BranchState<Integer> state) {
        List<Relationship> results = new ArrayList<>();

        List<Relationship> relationships = Lists.newArrayList(path.endNode().getRelationships(Direction.OUTGOING, withName(TransportRelationshipTypes.GOES_TO.name())));
        int currentTime = state.getState();
        String currentService = null;
        if (path.lastRelationship() != null) {
            currentTime = currentTime + (int) path.lastRelationship().getProperty("cost");
            currentService = (String) path.lastRelationship().getProperty("service_id");
        }

        List<Relationship> sameService = new ArrayList<>();

        for (Relationship r : relationships) {

            if (currentService != null && r.getProperty("service_id").toString().equals(currentService)) {
                sameService.add(r);
            }
        }


//        if (sameService.size() > 0) {
//            return sameService;
//        }

        for (Relationship r : relationships) {
            boolean[] days = (boolean[]) r.getProperty("days");
            int[] times = (int[]) r.getProperty("times");
            if (operatesOnday(days, today) && operatesOnTime(times, currentTime)) {
                results.add(r);
            }
        }

        return results;
    }

    private boolean operatesOnTime(int[] times, int currentTime) {
        for (int i = 0; i < times.length - 1; i++) {
            if (currentTime >= times[i] && currentTime <= times[i + 1]) {
                return true;
            }
        }
        return false;
    }

    private boolean operatesOnday(boolean[] days, DaysOfWeek today) {
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
    public PathExpander<Integer> reverse() {
        return this;
    }
}

