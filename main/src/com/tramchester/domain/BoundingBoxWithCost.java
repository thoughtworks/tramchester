package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;

import java.time.Duration;


public class BoundingBoxWithCost extends BoundingBox {

    private final Duration duration;
    private final Journey journey;

    public BoundingBoxWithCost(BoundingBox box, Duration duration, Journey journey) {
        super(box);
        this.duration = duration;
        this.journey = journey;
    }

    public Duration getDuration() {
        return duration;
    }

    public Journey getJourney() {
        return journey;
    }
}
