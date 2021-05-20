package com.tramchester.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.presentation.LatLong;

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

    public boolean within(long marginInKM, GridPosition position) {
        if (!position.isValid()) {
            throw new RuntimeException("Invalid grid position " + position);
        }
                return (position.getEastings() >= getMinEastings()-marginInKM) &&
                        (position.getEastings() <= getMaxEasting()+marginInKM) &&
                        (position.getNorthings() >= getMinNorthings()-marginInKM) &&
                        (position.getNorthings() <= getMaxNorthings()+marginInKM);
    }


    public boolean contained(LatLong destination) {
        return contained(CoordinateTransforms.getGridPosition(destination));
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
        if (other.bottomLeft.getNorthings()>topRight.getNorthings()) {
            return false;
        }
        if (other.topRight.getNorthings()<bottomLeft.getNorthings()) {
            return false;
        }
        if (other.bottomLeft.getEastings()>topRight.getEastings()) {
            return false;
        }
        if (other.topRight.getEastings()<bottomLeft.getEastings()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "bottomLeft=" + bottomLeft +
                ", topRight=" + topRight +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingBox that = (BoundingBox) o;

        if (!bottomLeft.equals(that.bottomLeft)) return false;
        return topRight.equals(that.topRight);
    }

    @Override
    public int hashCode() {
        int result = bottomLeft.hashCode();
        result = 31 * result + topRight.hashCode();
        return result;
    }

}
