package com.tramchester.geo;

public class GridPosition implements HasGridPosition {
    private final long eastings;
    private final long northings;

    public GridPosition(long eastings, long northings) {
        this.eastings = eastings;
        this.northings = northings;
    }

    public static GridPosition invalid() {
        return new GridPosition(-1,-1);
    }

    public long getEastings() {
        return eastings;
    }

    public long getNorthings() {
        return northings;
    }


    @Override
    public String toString() {
        return "GridPosition{" +
                "easting=" + eastings +
                ", northing=" + northings +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GridPosition that = (GridPosition) o;

        if (getEastings() != that.getEastings()) return false;
        return getNorthings() == that.getNorthings();
    }

    @Override
    public int hashCode() {
        int result = (int) (getEastings() ^ (getEastings() >>> 32));
        result = 31 * result + (int) (getNorthings() ^ (getNorthings() >>> 32));
        return result;
    }
}
