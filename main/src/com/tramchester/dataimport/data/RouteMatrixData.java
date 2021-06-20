package com.tramchester.dataimport.data;

import java.util.List;

public class RouteMatrixData {
    private Integer source;
    private List<Integer> destinations;

    public RouteMatrixData() {
        // deserialize
    }

    public RouteMatrixData(int source, List<Integer> destinations) {
        this.source = source;
        this.destinations = destinations;
    }


    public Integer getSource() {
        return source;
    }

    public List<Integer> getDestinations() {
        return destinations;
    }
}
