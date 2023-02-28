package com.tramchester.domain.id;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tramchester.domain.CoreDomain;
import com.tramchester.mappers.serialisation.IdForSerializer;
import org.jetbrains.annotations.NotNull;

@JsonSerialize(using = IdForSerializer.class)
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
