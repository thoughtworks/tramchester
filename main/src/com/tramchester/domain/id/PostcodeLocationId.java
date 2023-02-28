package com.tramchester.domain.id;

import com.tramchester.domain.places.PostcodeLocation;

import java.util.Objects;

public class PostcodeLocationId implements IdFor<PostcodeLocation> {

    private final IdFor<PostcodeLocation> contained;

    private PostcodeLocationId(String text) {
        contained = StringIdFor.createId(text, PostcodeLocation.class);
    }

    public static PostcodeLocationId create(String text) {
        return new PostcodeLocationId(text.toUpperCase());
    }

    @Deprecated
    @Override
    public String forDTO() {
        return contained.forDTO();
    }

    @Override
    public String getGraphId() {
        return contained.getGraphId();
    }

    @Override
    public boolean isValid() {
        return contained.isValid();
    }

    @Override
    public Class<PostcodeLocation> getDomainType() {
        return PostcodeLocation.class;
    }

    public String getName() {
        return contained.forDTO();
    }

    @Override
    public String toString() {
        return "PostcodeLocationId{" +
                contained +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostcodeLocationId that = (PostcodeLocationId) o;
        return contained.equals(that.contained);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contained);
    }
}
