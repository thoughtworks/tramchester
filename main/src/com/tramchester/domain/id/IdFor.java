package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tramchester.domain.CoreDomain;
import org.jetbrains.annotations.NotNull;

@JsonDeserialize(as= StringIdFor.class)
public interface IdFor<T extends CoreDomain> extends Comparable<IdFor<T>> {

    static <T extends CoreDomain> IdFor<T> invalid() {
        return new InvalidId<>();
    }

    @Deprecated
    String forDTO();

    String getGraphId();

    boolean isValid();

    @Override
    default int compareTo(@NotNull IdFor<T> o) {
        return this.getGraphId().compareTo(o.getGraphId());
    }

}
