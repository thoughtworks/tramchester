package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.BitmapAsRoaringBitmap;
import com.tramchester.domain.collections.SimpleBitmap;
import com.tramchester.domain.collections.SimpleImmutableBitmap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class BitmapAsRoaringBitmapTest {

    public static final int SIZE = 9;
    private static final int ROWS = 3;
    private static final int COLS = 4;
    private BitmapAsRoaringBitmap simpleBitmap;

    @BeforeEach
    public void onceBeforeEachTestRuns() {
        simpleBitmap = new BitmapAsRoaringBitmap(SIZE);
    }

    // See also IndexedBitSetTest

    @Test
    void shouldAllBeUnsetOnCreation() {
        for (int i = 0; i < SIZE; i++) {
            assertFalse(simpleBitmap.get(i));
        }
    }

    @Test
    void shouldHaveSize() {
        assertEquals(SIZE, simpleBitmap.size());
    }

    @Test
    void shouldSetBits() {
        for (int i = 0; i < SIZE; i++) {
            simpleBitmap.set(i);
            assertTrue(simpleBitmap.get(i));
            simpleBitmap.set(i, false);
            assertFalse(simpleBitmap.get(i));
        }
    }

    @Test
    void shouldHaveCardinality() {
        assertEquals(0,simpleBitmap.cardinality());
        for (int i = 0; i < SIZE; i++) {
            simpleBitmap.set(i);
            assertEquals(i+1, simpleBitmap.cardinality());
        }
    }

    @Test
    void shouldOr() {
        SimpleBitmap other = simpleBitmap.createCopy();
        other.set(4);
        other.set(6);
        simpleBitmap.or(other);
        assertTrue(simpleBitmap.get(4));
        assertTrue(simpleBitmap.get(6));
        assertEquals(2, simpleBitmap.cardinality());
    }

    @Test
    void shouldAnd() {
        SimpleBitmap other = simpleBitmap.createCopy();
        other.set(4);
        simpleBitmap.set(6);
        simpleBitmap.and(other);
        assertEquals(0, simpleBitmap.cardinality());

        other.set(6);
        simpleBitmap.set(6);
        simpleBitmap.and(other);
        assertTrue(simpleBitmap.get(6));
        assertEquals(1, simpleBitmap.cardinality());
    }

    @Test
    void shouldGetSubset() {
        simpleBitmap.set(1);
        simpleBitmap.set(4);
        simpleBitmap.set(6);
        simpleBitmap.set(8);
        SimpleImmutableBitmap submap = simpleBitmap.getSubmap(1, 5);

        assertFalse(submap.isEmpty());
        assertEquals(5, submap.size());

        assertTrue(submap.get(0), submap.toString());
        assertTrue(submap.get(3), submap.toString());
        assertEquals(2, submap.cardinality(), submap.toString());

        Set<Integer> submapStream = submap.getBitIndexes().map(Short::intValue).collect(Collectors.toSet());
        assertEquals(2, submapStream.size(), submapStream.toString());
        assertTrue(submapStream.contains(0));
        assertTrue(submapStream.contains(3));
    }

    @Test
    void shouldGetSubsetAllSet() {
        simpleBitmap.setAll(0, simpleBitmap.size());
        SimpleImmutableBitmap submap = simpleBitmap.getSubmap(1, 5);

        assertFalse(submap.isEmpty());
        assertEquals(5, submap.size());

        assertEquals(5, submap.cardinality(), submap.toString());


    }

    @Test
    void shouldCopy() {
        simpleBitmap.set(1);
        simpleBitmap.set(4);
        SimpleBitmap other = simpleBitmap.createCopy();

        assertTrue(other.get(1));
        assertTrue(other.get(4));
        assertEquals(2, other.cardinality(), other.toString());
    }

    @Test
    void shouldStream() {
        simpleBitmap.set(1);
        simpleBitmap.set(4);
        List<Integer> results = simpleBitmap.getBitIndexes().map(Short::intValue).collect(Collectors.toList());

        assertEquals(2, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(4));
    }

    @Test
    void shouldAndNot() {
        SimpleBitmap other = simpleBitmap.createCopy();
        simpleBitmap.set(1);
        simpleBitmap.set(2);
        other.set(1);
        other.set(4);

        simpleBitmap.andNot(other);
        assertTrue(simpleBitmap.get(2), simpleBitmap.toString());
        assertEquals(1, simpleBitmap.cardinality(), simpleBitmap.toString());
    }

    @Test
    void shouldSetFromArray() {
        int size = 3;
        int[] positionsToSet = new int[size];
        positionsToSet[0] = 4;
        positionsToSet[1] = 6;
        positionsToSet[2] = 7;

        simpleBitmap.set(positionsToSet);
        assertEquals(3, simpleBitmap.cardinality());
        assertTrue(simpleBitmap.get(4));
        assertTrue(simpleBitmap.get(6));
        assertTrue(simpleBitmap.get(7));

    }

    @Test
    void shouldExtractRowAndColum() {
        // assume 3 x 3 model for rows/columns
        // 0 1 2
        // 3 4 5
        // 6 7 8

        simpleBitmap.set(0);
        simpleBitmap.set(3);
        simpleBitmap.set(4);
        simpleBitmap.set(5);
        simpleBitmap.set(7);
        simpleBitmap.set(8);

        SimpleBitmap result = simpleBitmap.copyRowAndColumn(1, 1, 3, 3);
        assertEquals(4, result.cardinality(), result.toString());
        assertTrue(result.get(3), result.toString());
        assertTrue(result.get(4), result.toString());
        assertTrue(result.get(5), result.toString());
        assertTrue(result.get(7), result.toString());

    }

    @Test
    void testExtractRowAndColumnNotSquare() {

        BitmapAsRoaringBitmap rectBitmap = new BitmapAsRoaringBitmap(ROWS * COLS);

        // 1000
        // 0110
        // 0001

        set(rectBitmap,0,0);
        set(rectBitmap,1,1);
        set(rectBitmap,1,2);
        set(rectBitmap, 2,3);

        SimpleBitmap resultA = rectBitmap.copyRowAndColumn(0,1, ROWS, COLS);

        assertTrue(get(resultA, 0,0), resultA + " source:" + rectBitmap);
        assertTrue(get(resultA,1,1), resultA + " source:" + rectBitmap);
        assertEquals(2, resultA.cardinality(), resultA + " source:" + rectBitmap);

        SimpleBitmap resultB = rectBitmap.copyRowAndColumn(1,1, ROWS, COLS);
        assertEquals(2, resultB.cardinality(), resultB + " source:" + rectBitmap);
        assertTrue(get(resultB,1,2), resultB + " source:" + rectBitmap);
        assertTrue(get(resultB, 1,1), resultB + " source:" + rectBitmap);

        SimpleBitmap resultC = rectBitmap.copyRowAndColumn(2,0, ROWS, COLS);

        assertEquals(2, resultC.cardinality(), resultC + " source:" + rectBitmap);
        assertTrue(get(resultC,0,0));
        assertTrue(get(resultC,2,3));
    }

    private void set(SimpleBitmap bitmap, int row, int column) {
        final int position = SimpleBitmap.getPositionFor(row, column, ROWS, COLS);
        bitmap.set(position);
    }

    private boolean get(SimpleBitmap bitmap, int row, int column) {
        final int position = SimpleBitmap.getPositionFor(row, column, ROWS, COLS);
        return bitmap.get(position);
    }



}
