package com.tramchester.geo;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;

@Valid
public class BoundingBox {

    // whole uk, approx
    //    minEastings: 112685
    //    minNorthings: 015490
    //    maxEasting: 619307
    //    maxNorthings: 1108843

    private final GridPosition bottomLeft;
    private final GridPosition topRight;

    public BoundingBox(@JsonProperty(value = "minEastings", required = true) long minEastings,
                       @JsonProperty(value = "minNorthings", required = true) long minNorthings,
                       @JsonProperty(value = "maxEasting", required = true) long maxEasting,
                       @JsonProperty(value = "maxNorthings", required = true) long maxNorthings) {
        this(new GridPosition(minEastings, minNorthings), new GridPosition(maxEasting, maxNorthings));
    }

    public BoundingBox(GridPosition bottomLeft, GridPosition topRight) {
        this.bottomLeft = bottomLeft;
        this.topRight = topRight;
    }

    public BoundingBox(BoundingBox other) {
        this(other.bottomLeft, other.topRight);
    }

    public GridPosition getBottomLeft() {
        return bottomLeft;
    }

    public GridPosition getTopRight() {
        return topRight;
    }

    public long getMinEastings() {
        return bottomLeft.getEastings();
    }

    public long getMinNorthings() {
        return bottomLeft.getNorthings();
    }

    public long getMaxEasting() {
        return topRight.getEastings();
    }

    public long getMaxNorthings() {
        return topRight.getNorthings();
    }

    public boolean within(long margin, GridPosition position) {
        if (!position.isValid()) {
            throw new RuntimeException("Invalid grid position " + position);
        }
                return (position.getEastings() >= getMinEastings()-margin) &&
                        (position.getEastings() <= getMaxEasting()+margin) &&
                        (position.getNorthings() >= getMinNorthings()-margin) &&
                        (position.getNorthings() <= getMaxNorthings()+margin);
    }

    public boolean contained(GridPosition position) {
        if (!position.isValid()) {
            throw new RuntimeException("Invalid grid position " + position);
        }

        return (position.getEastings() >= getMinEastings()) &&
                (position.getEastings() <= getMaxEasting()) &&
                (position.getNorthings() >= getMinNorthings()) &&
                (position.getNorthings() <= getMaxNorthings());
    }

    public boolean overlapsWith(BoundingBox other) {
        GridPosition topLeft = new GridPosition(getMinEastings(), getMaxNorthings());
        GridPosition bottomRight = new GridPosition(getMaxEasting(), getMinNorthings());

        if (other.contained(bottomLeft)) {
            return true;
        }
        if (other.contained(topLeft)) {
            return true;
        }
        if (other.contained(topRight)) {
            return true;
        }
        return other.contained(bottomRight);

    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "bottomLeft=" + bottomLeft +
                ", topRight=" + topRight +
                '}';
    }
}
