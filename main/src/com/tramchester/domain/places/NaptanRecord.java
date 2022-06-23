package com.tramchester.domain.places;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.HasId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.IdSet;
import com.tramchester.domain.presentation.LatLong;
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
    private final LatLong latlong;

    private List<String> stopAreaCodes;

    public NaptanRecord(IdFor<NaptanRecord> id, String name, GridPosition gridPosition, LatLong latlong, String suburb, String town,
                        NaptanStopType stopType) {
        this.id = id;
        this.name = name;
        this.gridPosition = gridPosition;
        this.latlong = latlong;
        this.suburb = suburb;
        this.town = town;
        this.stopType = stopType;
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

    public GridPosition getGridPosition() {
        return gridPosition;
    }

    public LatLong getLatLong() {
        return latlong;
    }

    /***
     * @return the id's for the StopArea's associated with this stop/station/location
     */
    public IdSet<NaptanArea> getAreaCodes() {
        return stopAreaCodes.stream().map(NaptanArea::createId).collect(IdSet.idCollector());
    }

    @Override
    public String toString() {
        return "NaptanRecord{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", gridPosition=" + gridPosition +
                ", suburb='" + suburb + '\'' +
                ", town='" + town + '\'' +
                ", stopType=" + stopType +
                ", stopAreaCodes=" + stopAreaCodes +
                '}';
    }

    public void setAreaCodes(List<String> stopAreaCodes) {
        this.stopAreaCodes = stopAreaCodes;
    }
}
