package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;

public class InvalidId<T extends CoreDomain> implements IdFor<T> {

    private final Class<T> domainType;

    public InvalidId(Class<T> domainType) {

        this.domainType = domainType;
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
    public Class<T> getDomainType() {
        return domainType;
    }

    @Override
    public String toString() {
        return "InvalidId";
    }
}
