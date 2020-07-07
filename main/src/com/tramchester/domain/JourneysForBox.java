package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;

import java.util.List;

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
}
