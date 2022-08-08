package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.collections.SimpleListSingleton;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleListTest {

    private final Class<Integer> theClass = Integer.class;

    @Test
    void shouldAddSomethingToASingletonList() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(42);

        assertEquals(1, listA.size());

        SimpleList<Integer> listN = SimpleList.concat(listA, listA);

        assertEquals(2, listN.size());

    }

    @Test
    void shouldAddSomethingToAList() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(5);
        SimpleList<Integer> listC = new SimpleListSingleton<>(7);
        SimpleList<Integer> listD = new SimpleListSingleton<>(13);

        SimpleList<Integer> listM = SimpleList.concat(listA, listB);
        SimpleList<Integer> listN = SimpleList.concat(listM, listC);
        SimpleList<Integer> listO = SimpleList.concat(listN, listD);

        assertEquals(2, listM.size());
        assertEquals(3, listN.size());
        assertEquals(4, listO.size());

    }

    @Test
    void shouldStream() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(5);
        SimpleList<Integer> listC = new SimpleListSingleton<>(7);
        SimpleList<Integer> listD = new SimpleListSingleton<>(13);

        SimpleList<Integer> listM = SimpleList.concat(listA, listB);
        SimpleList<Integer> listN = SimpleList.concat(listM, listC);
        SimpleList<Integer> listO = SimpleList.concat(listN, listD);

        List<Integer> streamM = listM.stream().collect(Collectors.toList());
        assertEquals(2, streamM.size());
        assertTrue(streamM.contains(3));
        assertTrue(streamM.contains(5));

        List<Integer> streamO = listO.stream().collect(Collectors.toList());
        assertEquals(4, streamO.size());
        assertTrue(streamO.contains(3));
        assertTrue(streamO.contains(5));
        assertTrue(streamO.contains(7));
        assertTrue(streamO.contains(13));

    }

    //@Disabled("Performance test only")
    @Test
    void shouldConcatPerformanceTest() {
        final SimpleList<Integer> listA = new SimpleListSingleton<>(3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(5);

        for (int i = 0; i < 200000; i++) {
            listB = listB.concat(listA);
        }

        assertEquals(200000 + 1, listB.size());
    }

    @Test
    void shouldHaveOrdering() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(5);

        List<Integer> resultA = listA.concat(listB).stream().collect(Collectors.toList());

        List<Integer> expected = Arrays.asList(3, 5);
        assertEquals(expected, resultA);

        List<Integer> resultOtherOrder = Arrays.asList(5,3);

        List<Integer> resultB = listB.concat(listA).stream().collect(Collectors.toList());
        assertEquals(resultOtherOrder, resultB);

    }

    @Test
    void shouldHaveOrderingLongerList() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(5);
        SimpleList<Integer> listC = new SimpleListSingleton<>(7);
        SimpleList<Integer> listD = new SimpleListSingleton<>(13);

        SimpleList<Integer> listM = SimpleList.concat(listA, listB);
        SimpleList<Integer> listN = SimpleList.concat(listM, listC);
        SimpleList<Integer> listO = SimpleList.concat(listN, listD);

        List<Integer> result = listO.stream().collect(Collectors.toList());

        List<Integer> expected = Arrays.asList(3, 5, 7, 13);

        assertEquals(expected, result, listO.toString());

    }
}
