package com.tramchester.integration.testSupport;

import com.tramchester.domain.CoreDomain;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.id.StringIdFor;

public class Assertions {
    public static <T extends CoreDomain> void assertIdEquals(String text, IdFor<T> item) {
        org.junit.jupiter.api.Assertions.assertEquals(StringIdFor.createId(text, item.getDomainType()), item);
    }

    public static <T extends CoreDomain> void assertIdEquals(String text, IdFor<T> item, String message) {
        org.junit.jupiter.api.Assertions.assertEquals(StringIdFor.createId(text, item.getDomainType()), item, message);
    }

}
