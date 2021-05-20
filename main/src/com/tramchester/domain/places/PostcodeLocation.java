package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class PostcodeLocation implements Location<PostcodeLocation> {

    private final IdFor<PostcodeLocation> id;
    private final String name;

    private LatLong latLong;
    private GridPosition gridPosition;
    private final String area;

    public PostcodeLocation(LatLong latLong, String id, String area) {
        this.area = area;
        gridPosition = null;
        this.latLong = latLong;
        this.id = StringIdFor.createId(id);
        this.name = id;
    }

    public PostcodeLocation(GridPosition gridPosition, IdFor<PostcodeLocation> id, String area) {
        this.gridPosition = gridPosition;
        this.area = area;
        this.latLong = null;
        this.id = id;
        this.name = id.forDTO();
    }

    @Override
    public IdFor<PostcodeLocation> getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public LatLong getLatLong() {
        if (latLong==null) {
            latLong = CoordinateTransforms.getLatLong(gridPosition);
        }
        return latLong;
    }

    @Override
    public GridPosition getGridPosition() {
        if (gridPosition==null) {
            gridPosition = CoordinateTransforms.getGridPosition(latLong);
        }
        return gridPosition;
    }

    @Override
    public String getArea() {
        return area;
    }

    @Override
    public boolean hasPlatforms() {
        return false;
    }

    @Override
    public Set<Platform> getPlatforms() {
        return null;
    }

    @Override
    public Set<TransportMode> getTransportModes() {
        return Collections.singleton(TransportMode.Walk);
    }

    @Override
    public LocationType getLocationType() {
        return LocationType.Postcode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeLocation that = (PostcodeLocation) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public GraphPropertyKey getProp() {
        throw new RuntimeException("not graph property key for PostcodeLocation");
    }

    @Override
    public String toString() {
        return "PostcodeLocation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", latLong=" + latLong +
                ", gridPosition=" + gridPosition +
                ", area='" + area + '\'' +
                '}';
    }

    public String forDTO()  {
        return getId().forDTO();
    }
}
