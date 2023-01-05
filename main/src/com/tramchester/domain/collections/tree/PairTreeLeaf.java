package com.tramchester.domain.collections.tree;

import com.tramchester.domain.collections.RouteIndexPair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class PairTreeLeaf implements PairTree {
    private final RouteIndexPair leaf;
    private final PairTreeFactory factory;

    PairTreeLeaf(RouteIndexPair leaf, PairTreeFactory factory) {
        this.leaf = leaf;
        this.factory = factory;
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
    public Mutated replace(RouteIndexPair toReplace, RouteIndexPair pairA, RouteIndexPair pairB) {
        if (toReplace.equals(leaf)) {
            return Mutated.updated(factory.createBranch(pairA, pairB));
        }
        return Mutated.unchanged(this);
    }

    @Override
    public Set<PairTree> visit(PairTree.TreeVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public PairTreeId getId() {
        return new PairTreeId(leaf.getUniqueId());
    }

    public RouteIndexPair get() {
        return leaf;
    }

    @Override
    public String toString() {
        return "(" + leaf.first() + "," + leaf.second() + ")";
    }
}
