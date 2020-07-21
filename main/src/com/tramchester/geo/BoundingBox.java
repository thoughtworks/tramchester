package com.tramchester.geo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tramchester.domain.presentation.LatLong;
import org.opengis.referencing.operation.TransformException;

import javax.validation.Valid;

@Valid
public class BoundingBox {

    private final Long minEastings;
    private final Long minNorthings;
    private final Long maxEasting;
    private final Long maxNorthings;
    private final LatLong bottomLeft;
    private final LatLong topRight;

    public BoundingBox(@JsonProperty(value = "minEastings", required = true) long minEastings,
                       @JsonProperty(value = "minNorthings", required = true) long minNorthings,
                       @JsonProperty(value = "maxEasting", required = true) long maxEasting,
                       @JsonProperty(value = "maxNorthings", required = true) long maxNorthings) {
        this.minEastings = minEastings;
        this.minNorthings = minNorthings;
        this.maxEasting = maxEasting;
        this.maxNorthings = maxNorthings;

        try {
            bottomLeft = CoordinateTransforms.getLatLong(minEastings, minNorthings);
            topRight = CoordinateTransforms.getLatLong(maxEasting, maxNorthings);
        } catch (TransformException exception) {
            throw new RuntimeException("Cannot convert to lat/long", exception);
        }
    }

    public BoundingBox(BoundingBox other) {
        this(other.minEastings, other.minNorthings, other.maxEasting, other.maxNorthings);
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

    public boolean within(long margin, HasGridPosition position) {
                return (position.getEastings() >= minEastings-margin) &&
                        (position.getEastings() <= maxEasting+margin) &&
                        (position.getNorthings() >= minNorthings-margin) &&
                        (position.getNorthings() <= maxNorthings+margin);
    }

    public boolean contained(HasGridPosition position) {
        return (position.getEastings() >= minEastings) &&
                (position.getEastings() < maxEasting) &&
                (position.getNorthings() >= minNorthings) &&
                (position.getNorthings() < maxNorthings);
    }

    public boolean contained(LatLong latLong) {
        double lon = latLong.getLon();
        double lat = latLong.getLat();

        return (lon >= bottomLeft.getLon()) &&
                (lat >= bottomLeft.getLat()) &&
                (lon <= topRight.getLon()) &&
                (lat <= topRight.getLat());
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "minEastings=" + minEastings +
                ", minNorthings=" + minNorthings +
                ", maxEasting=" + maxEasting +
                ", maxNorthings=" + maxNorthings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingBox box = (BoundingBox) o;

        if (getMinEastings() != box.getMinEastings()) return false;
        if (getMinNorthings() != box.getMinNorthings()) return false;
        if (getMaxEasting() != box.getMaxEasting()) return false;
        return getMaxNorthings() == box.getMaxNorthings();
    }

    @Override
    public int hashCode() {
        int result = (int) (getMinEastings() ^ (getMinEastings() >>> 32));
        result = 31 * result + (int) (getMinNorthings() ^ (getMinNorthings() >>> 32));
        result = 31 * result + (int) (getMaxEasting() ^ (getMaxEasting() >>> 32));
        result = 31 * result + (int) (getMaxNorthings() ^ (getMaxNorthings() >>> 32));
        return result;
    }
}
