package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;

public class InvalidId<T extends GraphProperty> implements IdFor<T> {

    public InvalidId() {

    }

    @Override
    public String forDTO() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getGraphId() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public String toString() {
        return "InvalidId";
    }
}
