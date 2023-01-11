package com.tramchester.domain.collections.tree;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tramchester.domain.collections.RouteIndexPair;

import java.util.concurrent.TimeUnit;

// TODO Consider into Guice lifecycle control?

// TODO unclear if this actually helps
public class PairTreeFactory {

    private final Cache<PairTree.PairTreeId, PairTreeBranch> branches;
    private final Cache<PairTree.PairTreeId, PairTreeLeaf> leaves;


    public PairTreeFactory() {
        branches = Caffeine.newBuilder().maximumSize(50000).
                expireAfterAccess(10, TimeUnit.MINUTES).
                initialCapacity(4000).
                recordStats().build();

        leaves = Caffeine.newBuilder().maximumSize(50000).
                expireAfterAccess(10, TimeUnit.MINUTES).
                initialCapacity(4000).
                recordStats().build();
    }

    public PairTree createBranch(PairTree left, PairTree right) {
        PairTree.PairTreeId uniqueId = new PairTree.PairTreeId(left.getId(), right.getId());
        return branches.get(uniqueId, item -> new PairTreeBranch(left, right, this));
    }

    public PairTree createLeaf(RouteIndexPair pair) {
        PairTree.PairTreeId uniqueId = new PairTree.PairTreeId(pair.getUniqueId());
        return leaves.get(uniqueId, item -> new PairTreeLeaf(pair, this));
    }

    public PairTree createBranch(RouteIndexPair pairA, RouteIndexPair pairB) {
        PairTree left = createLeaf(pairA);
        PairTree right = createLeaf(pairB);
        return createBranch(left, right);
    }

}
