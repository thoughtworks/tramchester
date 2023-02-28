package com.tramchester.dataimport.data;

import com.tramchester.caching.CachableData;

import java.util.List;
import java.util.Objects;

public class CostsPerDegreeData implements CachableData {
    private int index;
    private int routeIndex;
    private List<Integer> setBits;

    public CostsPerDegreeData() {
        // deserialisation
    }

    public CostsPerDegreeData(int index, int routeIndex, List<Integer> setBits) {
        this.index = index;
        this.routeIndex = routeIndex;
        this.setBits = setBits;
    }

    public int getIndex() {
        return index;
    }

    public int getRouteIndex() {
        return routeIndex;
    }

    public List<Integer> getSetBits() {
        return setBits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CostsPerDegreeData that = (CostsPerDegreeData) o;
        return index == that.index && routeIndex == that.routeIndex && setBits.equals(that.setBits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, routeIndex, setBits);
    }
}
