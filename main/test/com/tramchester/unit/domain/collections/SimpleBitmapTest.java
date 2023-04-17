package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.SimpleBitmap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleBitmapTest {

    public static final int SIZE = 10;

    private static Stream<SimpleBitmap> getBitmaps() {
        return SimpleBitmap.getImplementationsOf(SIZE);
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldAllBeUnsetOnCreation(SimpleBitmap simpleBitmap) {
        for (int i = 0; i < SIZE; i++) {
            assertFalse(simpleBitmap.get(i));
        }
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldHaveSize(SimpleBitmap simpleBitmap) {
        assertEquals(SIZE, simpleBitmap.size());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldSetBits(SimpleBitmap simpleBitmap) {
        for (int i = 0; i < SIZE; i++) {
            simpleBitmap.set(i);
            assertTrue(simpleBitmap.get(i));
            simpleBitmap.set(i, false);
            assertFalse(simpleBitmap.get(i));
        }
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldHaveCardinality(SimpleBitmap simpleBitmap) {
        assertEquals(0,simpleBitmap.cardinality());
        for (int i = 0; i < SIZE; i++) {
            simpleBitmap.set(i);
            assertEquals(i+1, simpleBitmap.cardinality());
        }
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldOr(SimpleBitmap simpleBitmap) {
        SimpleBitmap other = simpleBitmap.createCopy();
        other.set(4);
        other.set(6);
        simpleBitmap.or(other);
        assertTrue(simpleBitmap.get(4));
        assertTrue(simpleBitmap.get(6));
        assertEquals(2, simpleBitmap.cardinality());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldAnd(SimpleBitmap simpleBitmap) {
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

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldGetSubset(SimpleBitmap simpleBitmap) {
        simpleBitmap.set(1);
        simpleBitmap.set(4);
        simpleBitmap.set(8);
        SimpleBitmap other = simpleBitmap.getSubmap(1, 5);

        assertTrue(other.get(0), other.toString());
        assertTrue(other.get(3), other.toString());
        assertEquals(2, other.cardinality(), other.toString());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldCopy(SimpleBitmap simpleBitmap) {
        simpleBitmap.set(1);
        simpleBitmap.set(4);
        SimpleBitmap other = simpleBitmap.createCopy();

        assertTrue(other.get(1));
        assertTrue(other.get(4));
        assertEquals(2, other.cardinality(), other.toString());
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldStream(SimpleBitmap simpleBitmap) {
        simpleBitmap.set(1);
        simpleBitmap.set(4);
        List<Integer> results = simpleBitmap.stream().boxed().collect(Collectors.toList());

        assertEquals(2, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(4));
    }

    @ParameterizedTest(name = "{displayName} {arguments}")
    @MethodSource("getBitmaps")
    void shouldAndNot(SimpleBitmap simpleBitmap) {
        SimpleBitmap other = simpleBitmap.createCopy();
        simpleBitmap.set(1);
        simpleBitmap.set(2);
        other.set(1);
        other.set(4);

        simpleBitmap.andNot(other);
        assertTrue(simpleBitmap.get(2), simpleBitmap.toString());
        assertEquals(1, simpleBitmap.cardinality(), simpleBitmap.toString());
    }
}
