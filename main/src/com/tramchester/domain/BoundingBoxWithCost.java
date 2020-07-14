package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;


public class BoundingBoxWithCost extends BoundingBox {

   private final int minutes;
    private final Journey journey;

    public BoundingBoxWithCost(BoundingBox box, int minutes, Journey journey) {
        super(box);
        this.minutes = minutes;
        this.journey = journey;
    }

    public int getMinutes() {
        return minutes;
    }

    public Journey getJourney() {
        return journey;
    }
}
