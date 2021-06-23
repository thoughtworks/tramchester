package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;
import com.tramchester.geo.GridPosition;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JourneysForBox {
    private final BoundingBox box;
    private final List<Journey> journeys;

    public JourneysForBox(BoundingBox box, List<Journey> journeys) {
        this.box = box;
        this.journeys = journeys;
    }

    public List<Journey> getJourneys() {
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
        if (journeys.size()==1) {
            return journeys.get(0);
        }
        return journeys.stream().min(Comparator.comparing(Journey::getArrivalTime)).
                orElseThrow(() -> new RuntimeException("Journeys empty"));
    }
}
