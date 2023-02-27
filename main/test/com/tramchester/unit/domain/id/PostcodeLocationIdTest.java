package com.tramchester.unit.domain.id;

import com.tramchester.domain.id.PostcodeLocationId;
import com.tramchester.testSupport.reference.TestPostcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PostcodeLocationIdTest {

    @Test
    public void shouldHaveEquality() {
        PostcodeLocationId idA = PostcodeLocationId.create(TestPostcodes.postcodeForPiccGardens());
        PostcodeLocationId idB = PostcodeLocationId.create(TestPostcodes.postcodeForPiccGardens());

        assertEquals(idA, idB);
        assertEquals(idB, idA);
    }

    @Test
    public void shouldHaveInEquality() {
        PostcodeLocationId idA = PostcodeLocationId.create(TestPostcodes.postcodeForPiccGardens());
        PostcodeLocationId idB = PostcodeLocationId.create(TestPostcodes.postcodeForWythenshaweHosp());

        assertNotEquals(idA, idB);
        assertNotEquals(idB, idA);
    }
}
