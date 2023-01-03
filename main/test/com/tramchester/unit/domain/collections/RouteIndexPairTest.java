package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.tree.PairTree;
import com.tramchester.domain.collections.tree.PairTreeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RouteIndexPairTest {

    private PairTreeFactory factory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        factory = new PairTreeFactory();
    }

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

    @Test
    void shouldReplaceAndFlatten() {
        RouteIndexPair pairA = RouteIndexPair.of(42, 85);
        RouteIndexPair pairB = RouteIndexPair.of(5, 9);
        RouteIndexPair pairC = RouteIndexPair.of(120, 99);

        PairTree tree = factory.createLeaf(pairA);
        assertEquals(Collections.singletonList(pairA), tree.flatten());

        PairTree.Mutated mutated = tree.replace(pairA, pairB, pairC);
        assertTrue(mutated.isChanged());
        tree = mutated.get();

        assertEquals(Arrays.asList(pairB, pairC), tree.flatten());

        RouteIndexPair pairD = RouteIndexPair.of(13,9);

        mutated = tree.replace(pairC, pairD, pairD);
        assertTrue(mutated.isChanged());
        tree = mutated.get();

        assertEquals(Arrays.asList(pairB, pairD, pairD), tree.flatten());

        mutated = tree.replace(RouteIndexPair.of(101, 100), RouteIndexPair.of(0, 0), RouteIndexPair.of(1, 1));
        assertFalse(mutated.isChanged());
        tree = mutated.get();
        assertEquals(Arrays.asList(pairB, pairD, pairD), tree.flatten());

    }

    @Test
    void shouldVisitSimple() {
        RouteIndexPair pairA = RouteIndexPair.of(1, 5);

        PairTree tree = factory.createLeaf(pairA);

        PairTree.TreeVisitor visitor = subTree -> {
            RouteIndexPair pair = subTree.get();
            RouteIndexPair pairAdd = RouteIndexPair.of(pair.first()+1, pair.second()+1);
            return Collections.singleton(factory.createLeaf(pairAdd));
        };

        List<PairTree> result = new ArrayList<>(tree.visit(visitor));

        assertEquals(1, result.size());

        PairTree resultTree = result.get(0);

        List<RouteIndexPair> leaves = resultTree.flatten();
        assertEquals(List.of(RouteIndexPair.of(2, 6)), leaves);
    }

    @Test
    void shouldVisitSimpleOneLevel() {
        RouteIndexPair pairA = RouteIndexPair.of(1, 2);
        RouteIndexPair pairB = RouteIndexPair.of(31, 35);
        RouteIndexPair pairC = RouteIndexPair.of(100, 105);

        PairTree tree = factory.createLeaf(pairA);
        tree = tree.replace(pairA, pairB, pairC).get();

        PairTree.TreeVisitor visitor = subTree -> {
            RouteIndexPair pair = subTree.get();
            RouteIndexPair pairAdd = RouteIndexPair.of(pair.first()+1, pair.second()+1);
            return Collections.singleton(factory.createLeaf(pairAdd));
        };

        List<PairTree> result = new ArrayList<>(tree.visit(visitor));

        assertEquals(1, result.size());

        PairTree resultTree = result.get(0);

        List<RouteIndexPair> leaves = resultTree.flatten();
        assertEquals(Arrays.asList(RouteIndexPair.of(32, 36), RouteIndexPair.of(101,106)), leaves);
    }

    @Test
    void shouldVisitPermutations() {
        RouteIndexPair pairA = RouteIndexPair.of(1, 2);
        RouteIndexPair pairB = RouteIndexPair.of(31, 35);
        RouteIndexPair pairC = RouteIndexPair.of(100, 105);

        PairTree original = factory.createLeaf(pairA);
        original = original.replace(pairA, pairB, pairC).get();

        PairTree.TreeVisitor visitor = subTree -> {
            RouteIndexPair pairAdd = RouteIndexPair.of(subTree.get().first()+1, subTree.get().second()+1);
            return Set.of(subTree, factory.createLeaf(pairAdd));
        };

        List<PairTree> result = new ArrayList<>(original.visit(visitor));

        assertEquals(4, result.size());

        List<List<RouteIndexPair>> lists = result.stream().map(PairTree::flatten).collect(Collectors.toList());

        assertTrue(lists.contains(Arrays.asList(RouteIndexPair.of(31,35), RouteIndexPair.of(100,105))));
        assertTrue(lists.contains(Arrays.asList(RouteIndexPair.of(31,35), RouteIndexPair.of(101,106))));
        assertTrue(lists.contains(Arrays.asList(RouteIndexPair.of(32,36), RouteIndexPair.of(100,105))));
        assertTrue(lists.contains(Arrays.asList(RouteIndexPair.of(32,36), RouteIndexPair.of(101,106))));

    }

    @Test
    void shouldVisitPermutationsWithDuplication() {
        RouteIndexPair pairA = RouteIndexPair.of(1, 2);
        RouteIndexPair pairB = RouteIndexPair.of(31, 35);
        RouteIndexPair pairC = RouteIndexPair.of(100, 105);

        PairTree original = factory.createLeaf(pairA);
        original = original.replace(pairA, pairB, pairC).get();

        PairTree.TreeVisitor visitor = tree -> {
            HashSet<PairTree> result = new HashSet<>();
            result.add(tree);
            result.add(tree);
            return result;
        };

        List<PairTree> result = new ArrayList<>(original.visit(visitor));

        assertEquals(1, result.size());

        List<List<RouteIndexPair>> lists = result.stream().map(PairTree::flatten).collect(Collectors.toList());

        assertTrue(lists.contains(Arrays.asList(RouteIndexPair.of(31,35), RouteIndexPair.of(100,105))));
        assertTrue(lists.contains(Arrays.asList(RouteIndexPair.of(31,35), RouteIndexPair.of(100,105))));

    }


}
