package com.tramchester.domain.places;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.repository.naptan.NaptanStopType;

public class NaptanRecord implements HasId<NaptanRecord> {
    private final IdFor<NaptanRecord> id;
    private final String name;
    private final GridPosition gridPosition;
    private final String suburb;
    private final String town;
    private final NaptanStopType stopType;

    public NaptanRecord(IdFor<NaptanRecord> id, String name, GridPosition gridPosition, String suburb, String town, NaptanStopType stopType) {
        this.id = id;
        this.name = name;
        this.gridPosition = gridPosition;
        this.suburb = suburb;
        this.town = town;
        this.stopType = stopType;
    }

    @Override
    public GraphPropertyKey getProp() {
        return null;
    }

    @Override
    public IdFor<NaptanRecord> getId() {
        return id;
    }

    public String getSuburb() {
        return suburb;
    }

    public String getName() {
        return name;
    }

    public NaptanStopType getStopType() {
        return stopType;
    }

    public String getTown() {
        return town;
    }

    public GridPosition getGridPosition() {
        return gridPosition;
    }
}
