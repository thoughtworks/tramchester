package com.tramchester.domain.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PairTreeLeaf implements PairTree {
    private final RouteIndexPair leaf;

    public PairTreeLeaf(RouteIndexPair leaf) {
        this.leaf = leaf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairTreeLeaf that = (PairTreeLeaf) o;
        return leaf.equals(that.leaf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leaf);
    }

    @Override
    public List<RouteIndexPair> flatten() {
        List<RouteIndexPair> result = new ArrayList<>();
        result.add(leaf);
        return result;
    }

    @Override
    public PairTree replace(RouteIndexPair toReplace, RouteIndexPair pairA, RouteIndexPair pairB) {
        if (toReplace.equals(leaf)) {
            PairTree left = new PairTreeLeaf(pairA);
            PairTree right = new PairTreeLeaf(pairB);
            return new PairTreeBranch(left, right);
        }
        return this;
    }

    @Override
    public Set<PairTree> visit(PairTree.TreeVisitor visitor) {
        return visitor.visit(this);
    }

    public RouteIndexPair get() {
        return leaf;
    }

    @Override
    public String toString() {
        return "(" + leaf.first() + "," + leaf.second() + ")";
    }
}