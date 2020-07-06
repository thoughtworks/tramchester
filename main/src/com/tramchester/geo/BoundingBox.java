package com.tramchester.geo;

import com.tramchester.dataimport.data.PostcodeData;

public class BoundingBox {
    private final long minEastings;
    private final long minNorthings;
    private final long maxEasting;
    private final long maxNorthings;

    public BoundingBox(long minEastings, long minNorthings, long maxEasting, long maxNorthings) {

        this.minEastings = minEastings;
        this.minNorthings = minNorthings;
        this.maxEasting = maxEasting;
        this.maxNorthings = maxNorthings;
    }

    public long getMinEastings() {
        return minEastings;
    }

    public long getMinNorthings() {
        return minNorthings;
    }

    public long getMaxEasting() {
        return maxEasting;
    }

    public long getMaxNorthings() {
        return maxNorthings;
    }

    public boolean within(HasGridPosition position, long margin) {
                return (position.getEastings() >= minEastings-margin) &&
                        (position.getEastings() <= maxEasting+margin) &&
                        (position.getNorthings() >= minNorthings-margin) &&
                        (position.getNorthings() <= maxNorthings+margin);
    }
}
