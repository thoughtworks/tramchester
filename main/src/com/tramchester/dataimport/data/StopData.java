package com.tramchester.dataimport.data;

import com.tramchester.domain.presentation.LatLong;
import com.tramchester.geo.GridPosition;

import java.util.Objects;

public class StopData {
    private final String id;
    private final String code;
    private final String area;
    private final String name;
    private final boolean isTram;

    private final LatLong latLong;

    // Need to do this once at first load so we have canonical grid position for each stop or station
    // otherwise lossy conversions to/from latlong cause issues elsewhere as may yeild different results
    private final GridPosition gridPosition;

    public StopData(String id, String code, String area, String name, double latitude, double longitude,
                    boolean isTram, GridPosition gridPosition) {
        this.id = id.intern();
        this.code = code.intern();
        this.area = area.intern();
        this.name = name.intern();
        this.gridPosition = gridPosition;
        this.latLong = new LatLong(latitude, longitude);
        this.isTram = isTram;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getArea() {
        return area;
    }

    public boolean isTFGMTram() {
        return isTram;
    }

    public LatLong getLatLong() {
        return latLong;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StopData stopData = (StopData) o;
        return Objects.equals(id, stopData.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StopData{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", area='" + area + '\'' +
                ", name='" + name + '\'' +
                ", isTram=" + isTram +
                ", latLong=" + latLong +
                ", gridPosition=" + gridPosition +
                '}';
    }

    public GridPosition getGridPosition() {
        return gridPosition;
    }
}
