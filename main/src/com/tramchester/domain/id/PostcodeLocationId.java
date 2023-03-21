package com.tramchester.domain.id;

import com.tramchester.domain.places.PostcodeLocation;

public class PostcodeLocationId extends ContainsId<PostcodeLocation> implements IdFor<PostcodeLocation> {

    private PostcodeLocationId(String text) {
        super(new StringIdFor<>(text, PostcodeLocation.class));
    }

    public static PostcodeLocationId create(String text) {
        return new PostcodeLocationId(text.toUpperCase());
    }

    @Override
    public String getGraphId() {
        return super.getGraphId();
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
        return "PostcodeLocationId{} " + super.toString();
    }

}
