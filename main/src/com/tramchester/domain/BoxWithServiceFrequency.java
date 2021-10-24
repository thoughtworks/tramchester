package com.tramchester.domain;

import com.tramchester.geo.BoundingBox;

public class BoxWithServiceFrequency extends BoundingBox {

    private final long numberOfStopCalls;

    public BoxWithServiceFrequency(BoundingBox box, long numberOfStopCalls) {
        super(box);
        this.numberOfStopCalls = numberOfStopCalls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BoxWithServiceFrequency that = (BoxWithServiceFrequency) o;

        return numberOfStopCalls == that.numberOfStopCalls;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (numberOfStopCalls ^ (numberOfStopCalls >>> 32));
        return result;
    }

    public long getNumberOfStopcalls() {
        return numberOfStopCalls;
    }

}
