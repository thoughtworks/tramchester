package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;

public interface IdFor<T extends GraphProperty> {
    String forDTO();

    String getGraphId();

    boolean notValid();

    boolean isValid();
}
