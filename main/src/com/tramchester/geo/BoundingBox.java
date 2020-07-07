package com.tramchester.geo;

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
