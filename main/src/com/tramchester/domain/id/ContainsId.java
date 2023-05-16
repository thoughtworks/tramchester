package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.CoreDomain;

abstract class ContainsId<T extends CoreDomain> implements IdFor<T> {

    @JsonIgnore
    abstract StringIdFor<T> getContainedId();

    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
