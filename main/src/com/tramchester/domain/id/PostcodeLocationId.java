package com.tramchester.domain.id;

import com.tramchester.domain.places.PostcodeLocation;

public class PostcodeLocationId implements IdFor<PostcodeLocation> {

    private final IdFor<PostcodeLocation> contained;

    private PostcodeLocationId(String text) {
        contained = StringIdFor.createId(text);
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

    public String getName() {
        return contained.forDTO();
    }
}
