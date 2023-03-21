package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.CoreDomain;

import java.util.Objects;

abstract class ContainsId<T extends CoreDomain> {
    private final StringIdFor<T> containedId;

    public ContainsId(StringIdFor<T> containedId) {
        this.containedId = containedId;
    }

    @JsonIgnore
    StringIdFor<T> getContainedId() {
        return containedId;
    }

    @JsonIgnore
    protected String getGraphId() {
        return containedId.getGraphId();
    }

    abstract protected boolean isValid() ;

    @Override
    public String toString() {
        return "ContainsId{" +
                "containedId=" + containedId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof ContainsId) {
            ContainsId<?> that = (ContainsId<?>) o;
            return Objects.equals(containedId, that.containedId);
        }
        if (o instanceof StringIdFor) {
            StringIdFor<?> that = (StringIdFor<?>) o;
            return Objects.equals(containedId, that);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(containedId);
    }
}
