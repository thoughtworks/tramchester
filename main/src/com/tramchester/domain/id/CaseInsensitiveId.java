package com.tramchester.domain.id;

import com.tramchester.domain.GraphProperty;

public class CaseInsensitiveId <C extends HasCaseInsensitiveId & GraphProperty> extends StringIdFor<C> {
    private CaseInsensitiveId(String theId) {
        super(theId.toUpperCase());
    }

    public static <C extends HasCaseInsensitiveId & GraphProperty> CaseInsensitiveId<C> createIdFor(String theId) {
        return new CaseInsensitiveId<>(theId.toUpperCase());
    }

}
