package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.collections.SimpleListEmpty;
import com.tramchester.domain.collections.SimpleListItems;
import com.tramchester.domain.collections.SimpleListSingleton;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SimpleListTest {

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
    void shouldHandleEmptyList() {
        SimpleList<Integer> empty = new SimpleListEmpty<>();
        assertEquals(0, empty.size());

        assertTrue(empty.isEmpty());

        SimpleList<Integer> singleton = new SimpleListSingleton<>(42);

        SimpleList<Integer> resultA = singleton.concat(empty);
        assertEquals(1, resultA.size());

        SimpleList<Integer> resultB = empty.concat(singleton);
        assertEquals(1, resultB.size());

        SimpleList<Integer> resultC = empty.concat(empty);
        assertEquals(0, resultC.size());

        SimpleList<Integer> listA = new SimpleListSingleton<>(3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(5);
        SimpleList<Integer> listM = SimpleList.concat(listA, listB);

        SimpleList<Integer> resultD = listM.concat(empty);
        assertEquals(2, resultD.size());
        assertFalse(resultD.isEmpty());

        SimpleList<Integer> resultE = empty.concat(listM);
        assertEquals(2, resultE.size());
        assertFalse(resultE.isEmpty());

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

    @Test
    void shouldBehaveWhenCreatedFromAnEmptyList() {
        SimpleList<Integer> simpleList = new SimpleListItems<>(Collections.emptyList());

        assertTrue(simpleList.isEmpty());
    }

    @Test
    void shouldCreateSimpleListFromAList() {
        List<Integer> numbers = Arrays.asList(9,8,7,6,5,4,3,2,1);

        SimpleList<Integer> result = new SimpleListItems<>(numbers);

        assertEquals(numbers.size(), result.size());

        List<Integer> resultList = result.toList();
        assertTrue(numbers.containsAll(resultList));

        assertEquals(numbers, resultList);

    }

    @Test
    void shouldHaveCollectors() {

        SimpleList<Integer> stream = Stream.of(2, 3, 4).sorted().collect(SimpleList.collector());
        assertEquals(3, stream.size());

        List<Integer> list = stream.stream().collect(Collectors.toList());
        assertEquals(2, list.get(0), list.toString());
        assertEquals(3, list.get(1), list.toString());
        assertEquals(4, list.get(2), list.toString());

    }

}
