package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tramchester.domain.CoreDomain;
import org.jetbrains.annotations.NotNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
public interface IdFor<T extends CoreDomain> extends Comparable<IdFor<T>> {

    static <T extends CoreDomain> IdFor<T> invalid(Class<T> domainType) {
        return new InvalidId<>(domainType);
    }

    @Deprecated
    String forDTO();

    String getGraphId();

    boolean isValid();

    Class<T> getDomainType();

    @Override
    default int compareTo(@NotNull IdFor<T> o) {
        return this.getGraphId().compareTo(o.getGraphId());
    }

}
