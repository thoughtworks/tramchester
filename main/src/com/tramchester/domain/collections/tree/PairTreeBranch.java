package com.tramchester.domain.collections.tree;

import com.tramchester.domain.collections.RouteIndexPair;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PairTreeBranch implements PairTree {
    private final PairTree left;
    private final PairTree right;
    private final int hash;

    PairTreeBranch(PairTree left, PairTree right) {
        this.left = left;
        this.right = right;
        hash = Objects.hash(left, right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairTreeBranch that = (PairTreeBranch) o;
        return left.equals(that.left) && right.equals(that.right);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public List<RouteIndexPair> flatten() {
        List<RouteIndexPair> leftResults = left.flatten();
        List<RouteIndexPair> rightResults = right.flatten();
        leftResults.addAll(rightResults);
        return leftResults;
    }

    @Override
    public PairTree replace(RouteIndexPair toReplace, RouteIndexPair pairA, RouteIndexPair pairB) {
        PairTree newLeft = left.replace(toReplace, pairA, pairB);
        PairTree newRight = right.replace(toReplace, pairA, pairB);
        return new PairTreeBranch(newLeft, newRight);
    }

    @Override
    public Set<PairTree> visit(PairTree.TreeVisitor visitor) {
        Set<PairTree> visitedLeft = left.visit(visitor);
        Set<PairTree> visitedRight = right.visit(visitor);

        return visitedLeft.stream().
                flatMap(visitLeft -> visitedRight.stream().map(visitRight -> new PairTreeBranch(visitLeft, visitRight))).
                collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "[ " + left.toString() + " , " + right.toString() + " ]";
    }
}
