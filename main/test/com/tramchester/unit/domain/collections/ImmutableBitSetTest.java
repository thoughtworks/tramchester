package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.BitmapAsBitset;
import com.tramchester.domain.collections.ImmutableBitSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ImmutableBitSetTest {

    @Test
    void shouldApplyAndNot() {
        int size = 8;

        BitmapAsBitset setA = new BitmapAsBitset(size);
        setA.set(1, true);
        setA.set(2, true);

        BitmapAsBitset setToApply = new BitmapAsBitset(size);
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

        BitmapAsBitset setA = new BitmapAsBitset(size);

        assertFalse(setA.get(1));

        BitmapAsBitset setB = new BitmapAsBitset(size);
        setB.set(1);
        ImmutableBitSet immutableBitSet = new ImmutableBitSet(setB, size);

        immutableBitSet.applyOrTo(setA);

        assertTrue(setA.get(1));
    }

    @Test
    void shouldCreate() {
        int size = 8;
        BitmapAsBitset set = new BitmapAsBitset(size);
        set.set(4);

        ImmutableBitSet immutableBitSetA = new ImmutableBitSet(set, size);

        assertTrue(immutableBitSetA.isSet(4));
        assertEquals(1, immutableBitSetA.numberSet());
        assertFalse(immutableBitSetA.isEmpty());

        ImmutableBitSet immutableBitSetB = new ImmutableBitSet(new BitmapAsBitset(size), size);
        assertEquals(0, immutableBitSetB.numberSet());
        assertTrue(immutableBitSetB.isEmpty());
    }
}
