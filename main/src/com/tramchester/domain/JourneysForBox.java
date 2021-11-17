package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;

import java.util.Comparator;
import java.util.Set;

public class JourneysForBox {
    private final BoundingBox box;
    private final Set<Journey> journeys;

    public JourneysForBox(BoundingBox box, Set<Journey> journeys) {
        this.box = box;
        this.journeys = journeys;
    }

    public Set<Journey> getJourneys() {
        return journeys;
    }

    public BoundingBox getBox() {
        return box;
    }

    @Override
    public String toString() {
        return "JourneysForBox{" +
                "box=" + box +
                ", journeys=" + journeys +
                '}';
    }

    public boolean contains(GridPosition destination) {
        return box.contained(destination);
    }

    public boolean isEmpty() {
        return journeys.isEmpty();
    }

    public Journey getLowestCost() {
//        if (journeys.size()==1) {
//            return journeys.get(0);
//        }
        return journeys.stream().min(Comparator.comparing(Journey::getArrivalTime)).
                orElseThrow(() -> new RuntimeException("Journeys empty"));
    }
}
