package com.tramchester.domain.id;

import com.tramchester.domain.places.PostcodeLocation;

import java.util.Objects;

public class PostcodeLocationId extends ContainsId<PostcodeLocation> implements IdFor<PostcodeLocation> {
    private final StringIdFor<PostcodeLocation> containedId;

    private PostcodeLocationId(String text) {
        containedId = new StringIdFor<>(text, PostcodeLocation.class);
    }

    public static PostcodeLocationId create(String text) {
        return new PostcodeLocationId(text.toUpperCase());
    }

    @Override
    public String getGraphId() {
        return containedId.getGraphId();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Class<PostcodeLocation> getDomainType() {
        return PostcodeLocation.class;
    }

    public String getName() {
        return getContainedId().getContainedId();
    }

    @Override
    public String toString() {
        return "PostcodeLocationId{" +
                "containedId=" + containedId +
                "}";
    }

    @Override
    StringIdFor<PostcodeLocation> getContainedId() {
        return containedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeLocationId that = (PostcodeLocationId) o;
        return containedId.equals(that.containedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(containedId);
    }
}
