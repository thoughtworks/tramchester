package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.SimpleList;
import com.tramchester.domain.collections.SimpleListSingleton;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleListTest {

    private final Class<Integer> theClass = Integer.class;

    @Test
    void shouldAddSomethingToASingletonList() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(theClass, 42);

        assertEquals(1, listA.size());

        SimpleList<Integer> listN = SimpleList.concat(Integer.class, listA, listA);

        assertEquals(2, listN.size());

    }

    @Test
    void shouldAddSomethingToAList() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(theClass, 3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(theClass, 5);
        SimpleList<Integer> listC = new SimpleListSingleton<>(theClass, 7);
        SimpleList<Integer> listD = new SimpleListSingleton<>(theClass, 13);

        SimpleList<Integer> listM = SimpleList.concat(Integer.class, listA, listB);
        SimpleList<Integer> listN = SimpleList.concat(Integer.class, listM, listC);
        SimpleList<Integer> listO = SimpleList.concat(Integer.class, listN, listD);

        assertEquals(2, listM.size());
        assertEquals(3, listN.size());
        assertEquals(4, listO.size());

    }

    @Test
    void shouldStream() {
        SimpleList<Integer> listA = new SimpleListSingleton<>(theClass, 3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(theClass, 5);
        SimpleList<Integer> listC = new SimpleListSingleton<>(theClass, 7);
        SimpleList<Integer> listD = new SimpleListSingleton<>(theClass, 13);

        SimpleList<Integer> listM = SimpleList.concat(Integer.class, listA, listB);
        SimpleList<Integer> listN = SimpleList.concat(Integer.class, listM, listC);
        SimpleList<Integer> listO = SimpleList.concat(Integer.class, listN, listD);

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
    void shouldConcatALotA() {
        final SimpleList<Integer> listA = new SimpleListSingleton<>(theClass, 3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(theClass, 5);

        for (int i = 0; i < 200000; i++) {
            listB = SimpleList.concat(Integer.class, listA, listB);
        }
    }

    //@Disabled("Performance test only")
    @Test
    void shouldConcatALotB() {
        final SimpleList<Integer> listA = new SimpleListSingleton<>(theClass, 3);
        SimpleList<Integer> listB = new SimpleListSingleton<>(theClass, 5);

        for (int i = 0; i < 200000; i++) {
            listB = listB.concat(listA);
        }
    }
}
