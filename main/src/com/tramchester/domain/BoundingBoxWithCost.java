package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;


public class BoundingBoxWithCost extends BoundingBox {

   private final int minutes;

    public BoundingBoxWithCost(BoundingBox box, int minutes) {
        super(box);
        this.minutes = minutes;
    }

    public int getMinutes() {
        return minutes;
    }
}
