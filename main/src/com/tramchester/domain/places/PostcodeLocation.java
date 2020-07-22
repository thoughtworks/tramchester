package com.tramchester.domain.places;

import com.tramchester.domain.HasId;
import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;
import java.util.Objects;

public class PostcodeLocation extends MapIdToDTOId<PostcodeLocation> implements Location, HasId<PostcodeLocation> {

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
    public List<Platform> getPlatforms() {
        return null;
    }

    @Override
    public TransportMode getTransportMode() {
        return TransportMode.Walk;
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
}
