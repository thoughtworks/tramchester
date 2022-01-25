package com.tramchester.domain.places;

import com.tramchester.geo.GridPosition;
import com.tramchester.geo.HasGridPosition;

public class NaptanArea implements HasGridPosition {
    private String name;
    private GridPosition gridPosition;

    public String getName() {
        return name;
    }

    @Override
    public GridPosition getGridPosition() {
        return gridPosition;
    }

}
