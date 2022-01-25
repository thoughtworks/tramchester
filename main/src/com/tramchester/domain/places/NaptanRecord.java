package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.geo.GridPosition;
import com.tramchester.repository.naptan.NaptanStopType;

import java.util.List;

public class NaptanRecord implements HasId<NaptanRecord>, CoreDomain {
    private final IdFor<NaptanRecord> id;
    private final String name;
    private final GridPosition gridPosition;
    private final String suburb;
    private final String town;
    private final NaptanStopType stopType;
    private final List<String> stopAreaCodes;

    public NaptanRecord(IdFor<NaptanRecord> id, String name, GridPosition gridPosition, String suburb, String town,
                        NaptanStopType stopType, List<String> stopAreaCodes) {
        this.id = id;
        this.name = name;
        this.gridPosition = gridPosition;
        this.suburb = suburb;
        this.town = town;
        this.stopType = stopType;
        this.stopAreaCodes = stopAreaCodes;
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

    /***
     * @return the id's for the StopArea's associated with this stop/station/location
     */
    public List<String> getAreaCodes() {
        return stopAreaCodes;
    }
}
