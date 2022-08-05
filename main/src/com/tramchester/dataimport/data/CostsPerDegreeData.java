package com.tramchester.dataimport.data;

import java.util.List;

public class CostsPerDegreeData {
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
}
