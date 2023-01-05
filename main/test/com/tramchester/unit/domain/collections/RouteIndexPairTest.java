package com.tramchester.unit.domain.collections;

import com.tramchester.domain.collections.RouteIndexPair;
import com.tramchester.domain.collections.RouteIndexPairFactory;
import com.tramchester.domain.collections.tree.PairTree;
import com.tramchester.domain.collections.tree.PairTreeFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class RouteIndexPairTest {

    public static final int NUMBER_OF_ROUTES = 1000;
    private PairTreeFactory treeFactory;
    private RouteIndexPairFactory indexPairFactory;

    @BeforeEach
    void onceBeforeEachTestRuns() {
        indexPairFactory = new RouteIndexPairFactory(() -> NUMBER_OF_ROUTES);
        treeFactory = new PairTreeFactory();
    }

    @NotNull
    private RouteIndexPair getPair(int first, int second) {
        return indexPairFactory.get(first, second);
    }

    @Test
    void shouldHaveAPair() {
        RouteIndexPair pair = getPair(42, 95);
        assertEquals(42, pair.first());
        assertEquals(95, pair.second());
    }


    @Test
    void shouldHaveSame() {
        RouteIndexPair pair = getPair(42, 42);
        assertTrue(pair.isSame());
    }

    @Test
    void  shouldGroupPairs() {
        RouteIndexPair pairA = getPair(3, 5);
        RouteIndexPair pairB = getPair(1, 3);
        RouteIndexPair pairC = getPair(4, 7);

        Stream<RouteIndexPair> pairsToGroup = Stream.of(pairA, pairC, pairB);

        List<RouteIndexPair.Group> grouped = RouteIndexPair.createAllUniqueGroups(pairsToGroup);
        assertEquals(1, grouped.size());

        RouteIndexPair.Group result = grouped.get(0);

        assertEquals(pairB, result.first());
        assertEquals(pairA, result.second());
    }

    @Test
    void shouldGenerateCombinations() {
        RouteIndexPair pairA = getPair(1, 2);
        RouteIndexPair pairB = getPair(2, 3);
        RouteIndexPair pairC = getPair(2, 4);
        RouteIndexPair pairD = getPair(2, 5);

        Stream<RouteIndexPair> pairsToGroup = Stream.of(pairA, pairD, pairB, pairC);

        List<RouteIndexPair.Group> groups = RouteIndexPair.createAllUniqueGroups(pairsToGroup);

        assertEquals(3, groups.size());
        assertTrue(groups.contains(RouteIndexPair.Group.of(pairA, pairB)), groups.toString());
        assertTrue(groups.contains(RouteIndexPair.Group.of(pairA, pairC)));
        assertTrue(groups.contains(RouteIndexPair.Group.of(pairA, pairD)));
    }

    @Test
    void shouldReplaceAndFlatten() {
        RouteIndexPair pairA = getPair(42, 85);
        RouteIndexPair pairB = getPair(5, 9);
        RouteIndexPair pairC = getPair(120, 99);

        PairTree tree = treeFactory.createLeaf(pairA);
        assertEquals(Collections.singletonList(pairA), tree.flatten());

        PairTree.Mutated mutated = tree.replace(pairA, pairB, pairC);
        assertTrue(mutated.isChanged());
        tree = mutated.get();

        assertEquals(Arrays.asList(pairB, pairC), tree.flatten());

        RouteIndexPair pairD = getPair(13, 9);

        mutated = tree.replace(pairC, pairD, pairD);
        assertTrue(mutated.isChanged());
        tree = mutated.get();

        assertEquals(Arrays.asList(pairB, pairD, pairD), tree.flatten());

        mutated = tree.replace(getPair(101, 100), getPair(0, 0), getPair(1, 1));
        assertFalse(mutated.isChanged());
        tree = mutated.get();
        assertEquals(Arrays.asList(pairB, pairD, pairD), tree.flatten());

    }

    @Test
    void shouldVisitSimple() {
        RouteIndexPair pairA = getPair(1, 5);

        PairTree tree = treeFactory.createLeaf(pairA);

        PairTree.TreeVisitor visitor = subTree -> {
            RouteIndexPair pair = subTree.get();
            RouteIndexPair pairAdd = getPair(pair.first() + 1, pair.second() + 1);
            return Collections.singleton(treeFactory.createLeaf(pairAdd));
            //return Stream.of(treeFactory.createLeaf(pairAdd));
        };

        List<PairTree> result = new ArrayList<>(tree.visit(visitor));

        assertEquals(1, result.size());

        PairTree resultTree = result.get(0);

        List<RouteIndexPair> leaves = resultTree.flatten();
        assertEquals(List.of(getPair(2, 6)), leaves);
    }

    @Test
    void shouldVisitSimpleOneLevel() {
        RouteIndexPair pairA = getPair(1, 2);
        RouteIndexPair pairB = getPair(31, 35);
        RouteIndexPair pairC = getPair(100, 105);

        PairTree tree = treeFactory.createLeaf(pairA);
        tree = tree.replace(pairA, pairB, pairC).get();

        PairTree.TreeVisitor visitor = subTree -> {
            RouteIndexPair pair = subTree.get();
            RouteIndexPair pairAdd = getPair(pair.first() + 1, pair.second() + 1);
            return Collections.singleton(treeFactory.createLeaf(pairAdd));
        };

        List<PairTree> result = new ArrayList<>(tree.visit(visitor));

        assertEquals(1, result.size());

        PairTree resultTree = result.get(0);

        List<RouteIndexPair> leaves = resultTree.flatten();
        assertEquals(Arrays.asList(getPair(32, 36), getPair(101, 106)), leaves);
    }

    @Test
    void shouldVisitPermutations() {
        RouteIndexPair pairA = getPair(1, 2);
        RouteIndexPair pairB = getPair(31, 35);
        RouteIndexPair pairC = getPair(100, 105);

        PairTree original = treeFactory.createLeaf(pairA);
        original = original.replace(pairA, pairB, pairC).get();

        PairTree.TreeVisitor visitor = subTree -> {
            RouteIndexPair pairAdd = getPair(subTree.get().first() + 1, subTree.get().second() + 1);
            return Set.of(subTree, treeFactory.createLeaf(pairAdd));
        };

        List<PairTree> result = new ArrayList<>(original.visit(visitor));

        assertEquals(4, result.size());

        List<List<RouteIndexPair>> lists = result.stream().map(PairTree::flatten).collect(Collectors.toList());

        assertTrue(lists.contains(Arrays.asList(getPair(31, 35), getPair(100, 105))));
        assertTrue(lists.contains(Arrays.asList(getPair(31, 35), getPair(101, 106))));
        assertTrue(lists.contains(Arrays.asList(getPair(32, 36), getPair(100, 105))));
        assertTrue(lists.contains(Arrays.asList(getPair(32, 36), getPair(101, 106))));

    }

    @Test
    void shouldVisitPermutationsWithDuplication() {
        RouteIndexPair pairA = getPair(1, 2);
        RouteIndexPair pairB = getPair(31, 35);
        RouteIndexPair pairC = getPair(100, 105);

        PairTree original = treeFactory.createLeaf(pairA);
        original = original.replace(pairA, pairB, pairC).get();

        PairTree.TreeVisitor visitor = tree -> {
//            return Stream.of(tree, tree);
            HashSet<PairTree> result = new HashSet<>();
            result.add(tree);
            result.add(tree);
            return result;
        };

        List<PairTree> result = new ArrayList<>(original.visit(visitor));

        assertEquals(1, result.size());

        List<List<RouteIndexPair>> lists = result.stream().map(PairTree::flatten).collect(Collectors.toList());

        assertTrue(lists.contains(Arrays.asList(getPair(31, 35), getPair(100, 105))));
        assertTrue(lists.contains(Arrays.asList(getPair(31, 35), getPair(100, 105))));

    }


}
