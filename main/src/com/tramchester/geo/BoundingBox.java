package com.tramchester.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.places.Location;
import com.tramchester.domain.presentation.LatLong;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.Set;

@Valid
public class BoundingBox {

    // whole uk, approx
    //    minEastings: 112685
    //    minNorthings: 015490
    //    maxEasting: 619307
    //    maxNorthings: 118843

    private final GridPosition bottomLeft;
    private final GridPosition topRight;

    public BoundingBox(@JsonProperty(value = "minEastings", required = true) long minEastings,
                       @JsonProperty(value = "minNorthings", required = true) long minNorthings,
                       @JsonProperty(value = "maxEasting", required = true) long maxEasting,
                       @JsonProperty(value = "maxNorthings", required = true) long maxNorthings) {
        this(new GridPosition(minEastings, minNorthings), new GridPosition(maxEasting, maxNorthings));
    }

    public BoundingBox(GridPosition bottomLeft, GridPosition topRight) {
        if (!bottomLeft.isValid()) {
            throw new RuntimeException("bottemLeft is invalid");
        }
        if (!topRight.isValid()) {
            throw new RuntimeException("topRight is invalid");
        }
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

    public boolean within(MarginInMeters marginInMeters, GridPosition position) {
        if (!position.isValid()) {
            throw new RuntimeException("Invalid grid position " + position);
        }

        long margin = marginInMeters.get();
        return (position.getEastings() >= getMinEastings() - margin) &&
                (position.getEastings() <= getMaxEasting() + margin) &&
                (position.getNorthings() >= getMinNorthings() - margin) &&
                (position.getNorthings() <= getMaxNorthings() + margin);
    }

    public boolean contained(Location<?> hasPosition) {
        return contained(hasPosition.getGridPosition());
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

    public Set<BoundingBox> quadrants() {
        Set<BoundingBox> result = new HashSet<>();
        final long left = bottomLeft.getEastings();
        final long top = topRight.getNorthings();
        final long bottom = bottomLeft.getNorthings();
        final long right = topRight.getEastings();

        long midEasting = left + ((right - left) / 2);
        long midNorthing = bottom +  ((top - bottom) / 2);

        GridPosition middle = new GridPosition(midEasting, midNorthing);

        BoundingBox bottomLeftQuadrant = new BoundingBox(bottomLeft, middle);
        BoundingBox topRightQuadrant = new BoundingBox(middle, topRight);

        BoundingBox topLeftQuadrant = new BoundingBox(
                new GridPosition(left, midNorthing), new GridPosition(midEasting, top));
        BoundingBox bottomRightQuadrant = new BoundingBox(
                new GridPosition(midEasting, bottom), new GridPosition(right, midNorthing));

        result.add(bottomLeftQuadrant);
        result.add(topRightQuadrant);
        result.add(topLeftQuadrant);
        result.add(bottomRightQuadrant);

        return result;
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

    public long width() {
        return Math.abs(bottomLeft.getEastings() - topRight.getEastings());
    }

    public long height() {
        return Math.abs(bottomLeft.getNorthings() - topRight.getNorthings());
    }

    public GridPosition getMidPoint() {
        long midEasting = (bottomLeft.getEastings() + topRight.getEastings()) / 2;
        long midNorthing = (bottomLeft.getNorthings() + topRight.getNorthings()) / 2;
        return new GridPosition(midEasting, midNorthing);
    }
}
