package com.tramchester.domain.places;

import com.tramchester.domain.DataSourceID;
import com.tramchester.domain.Platform;
import com.tramchester.domain.Route;
import com.tramchester.domain.id.CaseInsensitiveId;
import com.tramchester.domain.id.HasCaseInsensitiveId;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.geo.CoordinateTransforms;
import com.tramchester.geo.GridPosition;
import com.tramchester.graph.GraphPropertyKey;
import com.tramchester.graph.graphbuild.GraphLabel;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class PostcodeLocation implements Location<PostcodeLocation>, HasCaseInsensitiveId {

    private final CaseInsensitiveId<PostcodeLocation> id;
    private final String name;

    private LatLong latLong;
    private GridPosition gridPosition;

    public PostcodeLocation(LatLong latLong, CaseInsensitiveId<PostcodeLocation> id) {
        this(id, latLong, CoordinateTransforms.getGridPosition(latLong));
    }

    public PostcodeLocation(GridPosition gridPosition, CaseInsensitiveId<PostcodeLocation> id) {
        this(id, CoordinateTransforms.getLatLong(gridPosition), gridPosition);
    }

    private PostcodeLocation(CaseInsensitiveId<PostcodeLocation> id, LatLong latLong, GridPosition gridPosition) {
        this.id = id;
        this.name = id.forDTO();

        this.latLong = latLong;
        this.gridPosition = gridPosition;
    }

    @Override
    public CaseInsensitiveId<PostcodeLocation> getId() {
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
    public IdFor<NaptanArea> getAreaId() {
        return StringIdFor.invalid();
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
    public DataSourceID getDataSourceID() {
        return DataSourceID.postcode;
    }

    @Override
    public boolean hasPickup() {
        return true;
    }

    @Override
    public boolean hasDropoff() {
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Set<Route> getDropoffRoutes() {
        return Collections.emptySet();
    }

    @Override
    public Set<Route> getPickupRoutes() {
        return Collections.emptySet();
    }

    @Override
    public GraphLabel getNodeLabel() {
        throw new RuntimeException("Not implemented for postcode");
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
                '}';
    }

    public String forDTO()  {
        return getId().forDTO();
    }
}
