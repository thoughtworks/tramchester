package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;

public interface IdFor<T extends GraphProperty> {

    static <T extends GraphProperty> IdFor<T> invalid() {
        return new InvalidId<>();
    }

    String forDTO();

    String getGraphId();

    boolean isValid();
}
