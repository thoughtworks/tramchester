package com.tramchester.domain.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tramchester.domain.CoreDomain;

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
            return this.containedId.equals(that.containedId);
            //return Objects.equals(containedId, that.containedId);
        }
        if (o instanceof StringIdFor) {
            StringIdFor<?> that = (StringIdFor<?>) o;
            return this.containedId.equals(that);
            //return Objects.equals(containedId, that);
        }
        return false;
    }

    // NOTE need to use contained hash otherwise identity inside of "mixed" collections of id's does not work as expected
    @Override
    public int hashCode() {
        return containedId.hashCode();
    }
}
