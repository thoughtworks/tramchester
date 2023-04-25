package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.ImmutableBitSet;
import com.tramchester.domain.collections.IndexedBitSet;
import com.tramchester.domain.collections.SimpleBitmap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class IndexedBitSetTest {

    @Test
    void shouldHaveRowsAndColumns() {
        IndexedBitSet bits = new IndexedBitSet(3,4);

        // zero indexed
        bits.set(2,3);

        assertTrue(bits.isSet(2,3), bits.toString());

        ImmutableBitSet row3 = bits.getBitSetForRow(2);
        assertTrue(row3.isSet(3), bits + " " + row3);

        bits.set(1,1);
        assertTrue(bits.isSet(1,1), bits.toString());

        ImmutableBitSet row2 = bits.getBitSetForRow(1);
        assertTrue(row2.isSet(1), bits + " " + row3);

        bits.clear();
        assertFalse(bits.isSet(2,3));
        assertFalse(bits.isSet(1,1));

    }

    @Test
    void shouldHaveNumberOfBitsSet() {
        IndexedBitSet bits = new IndexedBitSet(3,4);

        assertEquals(0, bits.numberOfBitsSet());

        bits.set(0,0);
        assertEquals(1, bits.numberOfBitsSet());

        bits.set(0,1);
        assertEquals(2, bits.numberOfBitsSet());

        bits.set(2,2);
        assertEquals(3, bits.numberOfBitsSet());

        bits.clear();
        assertEquals(0, bits.numberOfBitsSet());

    }

    @Test
    void shouldHaveAllSet() {
        IndexedBitSet all = IndexedBitSet.getIdentity(3,4);
        assertEquals(12, all.numberOfBitsSet());
    }

    @Test
    void shouldApplyAnd() {
        IndexedBitSet bits = new IndexedBitSet(2,2);
        bits.set(1,1);
        bits.set(0,0);

        IndexedBitSet allSetMask = IndexedBitSet.getIdentity(2,2);

        IndexedBitSet result = bits.and(allSetMask);
        assertTrue(result.isSet(1,1));
        assertTrue(result.isSet(0,0));
        assertEquals(2, result.numberOfBitsSet());

        IndexedBitSet partialMask = new IndexedBitSet(2,2);

        partialMask.set(1,1);

        result = bits.and(partialMask);
        assertTrue(result.isSet(1,1), result.toString());
        assertFalse(result.isSet(0,0), result.toString());
        assertEquals(1, result.numberOfBitsSet());

        IndexedBitSet noneSet = new IndexedBitSet(2,2);

        result = bits.and(noneSet);
        assertFalse(result.isSet(1,1), result.toString());
        assertEquals(0, result.numberOfBitsSet());
    }

    @Test
    void shouldApplyAndToSingleRow() {
        IndexedBitSet bits = new IndexedBitSet(2,2);
        bits.set(1,1);
        bits.set(0,0);

        SimpleBitmap rowMaskAllSet = SimpleBitmap.create(2);
        rowMaskAllSet.setAll(0,2, true);
        bits.applyAndTo(1, rowMaskAllSet);

        assertTrue(bits.isSet(0,0));
        assertTrue(bits.isSet(1,1));

        SimpleBitmap rowMaskNonSet = SimpleBitmap.create(2);
        bits.applyAndTo(1, rowMaskNonSet);

        assertFalse(bits.isSet(1,1));
        assertTrue(bits.isSet(0,0));
    }

    @Test
    void shouldInsertBits() {
        IndexedBitSet bits = new IndexedBitSet(3,4);

        assertEquals(0, bits.numberOfBitsSet());

        SimpleBitmap rowToInsert = SimpleBitmap.create(4);
        rowToInsert.set(1);
        rowToInsert.set(3);
        bits.insert(1, rowToInsert);

        assertEquals(2, bits.numberOfBitsSet(), bits.toString());
        assertTrue(bits.isSet(1,1));
        assertTrue(bits.isSet(1,3));

    }

    @Test
    void shouldGetPairsForSetBits() {
        IndexedBitSet bits = new IndexedBitSet(3,4);

        bits.set(0,1);
        bits.set(0,3);

        Set<Pair<Integer, Integer>> pairs = bits.getPairs().collect(Collectors.toSet());
        assertEquals(2, pairs.size());
        assertTrue(pairs.contains(Pair.of(0,1)));
        assertTrue(pairs.contains(Pair.of(0,3)));

        bits.set(1,1);
        bits.set(2,3);

        pairs = bits.getPairs().collect(Collectors.toSet());

        assertEquals(4, pairs.size());
        assertTrue(pairs.contains(Pair.of(0,1)));
        assertTrue(pairs.contains(Pair.of(0,3)));
        assertTrue(pairs.contains(Pair.of(1,1)));
        assertTrue(pairs.contains(Pair.of(2,3)));

    }

    @Test
    void shouldExtractRowAndColumnBits() {
        IndexedBitSet bits = new IndexedBitSet(3,4);

        bits.set(0,0);
        bits.set(1,1);
        bits.set(1,2);
        bits.set(2,3);

        IndexedBitSet resultA = bits.getRowAndColumn(0,1);
        assertEquals(2, resultA.numberOfBitsSet(), resultA + " source:" + bits);
        assertTrue(resultA.isSet(0,0), resultA + " source:" + bits);
        assertTrue(resultA.isSet(1,1));

        IndexedBitSet resultB = bits.getRowAndColumn(1,1);
        assertEquals(2, resultB.numberOfBitsSet(), resultB + " source:" + bits);
        assertTrue(resultB.isSet(1,2), resultB + " source:" + bits);
        assertTrue(resultB.isSet(1,1), resultB + " source:" + bits);

        IndexedBitSet resultC = bits.getRowAndColumn(2,0);
        assertEquals(2, resultC.numberOfBitsSet(), resultC + " source:" + bits);
        assertTrue(resultC.isSet(0,0));
        assertTrue(resultC.isSet(2,3));

    }
}
