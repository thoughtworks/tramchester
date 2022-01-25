package com.tramchester.domain.id;

import com.tramchester.domain.CoreDomain;

public class CaseInsensitiveId <C extends HasCaseInsensitiveId & CoreDomain> extends StringIdFor<C> {
    private CaseInsensitiveId(String theId) {
        super(theId.toUpperCase());
    }

    public static <C extends HasCaseInsensitiveId & CoreDomain> CaseInsensitiveId<C> createIdFor(String theId) {
        return new CaseInsensitiveId<>(theId.toUpperCase());
    }

}
