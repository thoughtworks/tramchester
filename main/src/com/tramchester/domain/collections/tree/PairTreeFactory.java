package com.tramchester.domain.collections.tree;

import com.tramchester.domain.collections.RouteIndexPair;

import java.util.HashMap;
import java.util.Map;

// TODO Consider into Guice lifecycle control?

// TODO unclear if this actually helps
public class PairTreeFactory {

    private final Map<PairTree.PairTreeId, PairTree> elements;

    public PairTreeFactory() {
        elements = new HashMap<>();
    }

    public PairTree createBranch(PairTree left, PairTree right) {
        PairTree.PairTreeId uniqueId = new PairTree.PairTreeId(left.getId(), right.getId());
        if (elements.containsKey(uniqueId)) {
            return elements.get(uniqueId);
        }
        PairTreeBranch created = new PairTreeBranch(left, right, this);
        elements.put(uniqueId, created);
        return created;
    }

    public PairTree createLeaf(RouteIndexPair pair) {
        PairTree.PairTreeId uniqueId = new PairTree.PairTreeId(pair.getUniqueId());
        if (elements.containsKey(uniqueId)) {
            return elements.get(uniqueId);
        }
        PairTreeLeaf created = new PairTreeLeaf(pair, this);
        elements.put(uniqueId, created);
        return created;
    }

    public PairTree createBranch(RouteIndexPair pairA, RouteIndexPair pairB) {
        PairTree left = createLeaf(pairA);
        PairTree right = createLeaf(pairB);
        return createBranch(left, right);
    }

}
