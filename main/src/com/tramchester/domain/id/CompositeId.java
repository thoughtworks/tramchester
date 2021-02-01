package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;
import com.tramchester.graph.GraphPropertyKey;

public class CompositeId<T extends GraphProperty> implements HasId<T>{
    @Override
    public GraphPropertyKey getProp() {
        return null;
    }

    @Override
    public StringIdFor<T> getId() {
        return null;
    }
}
