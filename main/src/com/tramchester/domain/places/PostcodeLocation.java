package com.tramchester.domain.places;

import com.tramchester.domain.Platform;
import com.tramchester.domain.presentation.LatLong;

import java.util.List;
import java.util.Objects;

public class PostcodeLocation implements Location {

    private final LatLong LatLong;
    private final String id;

    public PostcodeLocation(com.tramchester.domain.presentation.LatLong latLong, String id) {
        LatLong = latLong;
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public LatLong getLatLong() {
        return LatLong;
    }

    @Override
    public boolean isTram() {
        return false;
    }

    @Override
    public String getArea() {
        if (id.length()==5) {
            return id.substring(0,2);
        }
        if (id.length()==6) {
            return id.substring(0,3);
        }
        if (id.length()==7) {
            return id.substring(0,4);
        }
        return id;
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
