package com.tramchester.domain.collections;

import java.util.List;
import java.util.Set;

public interface PairTree {
    List<RouteIndexPair> flatten();

    PairTree replace(RouteIndexPair toReplace, RouteIndexPair pairA, RouteIndexPair pairB);

    Set<PairTree> visit(TreeVisitor visitor);

    // TODO Mutated Flag passed in??

    public interface TreeVisitor {
        Set<PairTree> visit(PairTreeLeaf tree);
    }

}
