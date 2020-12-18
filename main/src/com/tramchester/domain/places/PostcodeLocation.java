package com.tramchester.domain.places;

import com.tramchester.domain.*;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.reference.TransportMode;
import com.tramchester.graph.GraphPropertyKey;

import java.util.Objects;
import java.util.Set;

public class PostcodeLocation extends MapIdToDTOId<PostcodeLocation> implements Location<PostcodeLocation> {

    private final LatLong LatLong;
    private final IdFor<PostcodeLocation> id;
    private final String name;

    public PostcodeLocation(com.tramchester.domain.presentation.LatLong latLong, String id) {
        LatLong = latLong;
        this.id = IdFor.createId(id);
        this.name = id;
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
        return LatLong;
    }

    @Override
    public String getArea() {
        if (name.length()==5) {
            return name.substring(0,2);
        }
        if (name.length()==6) {
            return name.substring(0,3);
        }
        if (name.length()==7) {
            return name.substring(0,4);
        }
        return name;
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
    public TransportMode getTransportMode() {
        return TransportMode.Walk;
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
                "LatLong=" + LatLong +
                ", id=" + id +
                ", name='" + name + '\'' +
                "} " + super.toString();
    }
}
