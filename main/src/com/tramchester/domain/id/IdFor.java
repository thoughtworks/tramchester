package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.config.StationClosureConfig;
import com.tramchester.domain.GraphProperty;
import org.jetbrains.annotations.NotNull;

@JsonDeserialize(as= StringIdFor.class)
public interface IdFor<T extends GraphProperty> extends Comparable<IdFor<T>> {

    static <T extends GraphProperty> IdFor<T> invalid() {
        return new InvalidId<>();
    }

    String forDTO();

    String getGraphId();

    boolean isValid();

    @Override
    default int compareTo(@NotNull IdFor<T> o) {
        return this.getGraphId().compareTo(o.getGraphId());
    }

}
