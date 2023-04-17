package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.ImmutableBitSet;
import com.tramchester.domain.collections.SimpleBitmap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ImmutableBitSetTest {

    @Test
    void shouldApplyAndNot() {
        int size = 8;

        SimpleBitmap setA = SimpleBitmap.create(size);
        setA.set(1, true);
        setA.set(2, true);

        SimpleBitmap setToApply = SimpleBitmap.create(size);
        setToApply.set(1);

        ImmutableBitSet immutableBitSet = new ImmutableBitSet(setToApply, size);

        assertTrue(setA.get(1));
        assertTrue(setA.get(2));

        immutableBitSet.applyAndNotTo(setA);

        assertFalse(setA.get(1));
        assertTrue(setA.get(2));
    }

    @Test
    void shouldApplyOr() {
        int size = 8;

        SimpleBitmap setA = SimpleBitmap.create(size);

        assertFalse(setA.get(1));

        SimpleBitmap setB = SimpleBitmap.create(size);
        setB.set(1);
        ImmutableBitSet immutableBitSet = new ImmutableBitSet(setB, size);

        immutableBitSet.applyOrTo(setA);

        assertTrue(setA.get(1));
    }

    @Test
    void shouldCreate() {
        int size = 8;
        SimpleBitmap set = SimpleBitmap.create(size);
        set.set(4);

        ImmutableBitSet immutableBitSetA = new ImmutableBitSet(set, size);

        assertTrue(immutableBitSetA.isSet(4));
        assertEquals(1, immutableBitSetA.numberSet());
        assertFalse(immutableBitSetA.isEmpty());

        ImmutableBitSet immutableBitSetB = new ImmutableBitSet(SimpleBitmap.create(size), size);
        assertEquals(0, immutableBitSetB.numberSet());
        assertTrue(immutableBitSetB.isEmpty());
    }
}
