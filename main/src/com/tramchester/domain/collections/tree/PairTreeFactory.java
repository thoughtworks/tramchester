package com.tramchester.domain.collections.tree;

import com.tramchester.domain.collections.RouteIndexPair;

// TODO Consider into Guice lifecycle control?
public class PairTreeFactory {

    public PairTree createBranch(PairTree left, PairTree right) {
        return new PairTreeBranch(left, right, this);
    }

    public PairTree createLeaf(RouteIndexPair pair) {
        return new PairTreeLeaf(pair, this);
    }
}
