package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.RouteIndexPair;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteIndexPairTest {

    @Test
    void shouldHaveAPair() {
        RouteIndexPair pair = RouteIndexPair.of(42,95);
        assertEquals(42, pair.first());
        assertEquals(95, pair.second());
    }

    @Test
    void shouldHaveSame() {
        RouteIndexPair pair = RouteIndexPair.of(42,42);
        assertTrue(pair.isSame());
    }

    @Test
    void  shouldGroupPairs() {
        RouteIndexPair pairA = RouteIndexPair.of(3,5);
        RouteIndexPair pairB = RouteIndexPair.of(1,3);
        RouteIndexPair pairC = RouteIndexPair.of(4,7);

        Stream<RouteIndexPair> pairsToGroup = Stream.of(pairA, pairC, pairB);

        List<RouteIndexPair.Group> grouped = RouteIndexPair.createAllUniqueGroups(pairsToGroup);
        assertEquals(1, grouped.size());

        RouteIndexPair.Group result = grouped.get(0);

        assertEquals(pairB, result.first());
        assertEquals(pairA, result.second());
    }

    @Test
    void shouldGenerateCombinations() {
        RouteIndexPair pairA = RouteIndexPair.of(1,2);
        RouteIndexPair pairB = RouteIndexPair.of(2,3);
        RouteIndexPair pairC = RouteIndexPair.of(2,4);
        RouteIndexPair pairD = RouteIndexPair.of(2,5);

        Stream<RouteIndexPair> pairsToGroup = Stream.of(pairA, pairD, pairB, pairC);

        List<RouteIndexPair.Group> groups = RouteIndexPair.createAllUniqueGroups(pairsToGroup);

        assertEquals(3, groups.size());
        assertTrue(groups.contains(RouteIndexPair.Group.of(pairA, pairB)), groups.toString());
        assertTrue(groups.contains(RouteIndexPair.Group.of(pairA, pairC)));
        assertTrue(groups.contains(RouteIndexPair.Group.of(pairA, pairD)));

    }
}
