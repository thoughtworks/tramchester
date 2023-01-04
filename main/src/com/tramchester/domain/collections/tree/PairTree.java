package com.tramchester.domain.collections.tree;

import com.tramchester.domain.collections.RouteIndexPair;

import java.util.List;
import java.util.Set;

public interface PairTree {
    List<RouteIndexPair> flatten();

    Mutated replace(RouteIndexPair toReplace, RouteIndexPair pairA, RouteIndexPair pairB);

    Set<PairTree> visit(TreeVisitor visitor);

    interface TreeVisitor {
        Set<PairTree> visit(PairTreeLeaf tree);
    }

    // TODO unclear is this is actually helping at all
    class Mutated {
        private final PairTree pairTree;
        private final boolean changed;

        private Mutated(PairTree pairTree, boolean changed) {
            this.pairTree = pairTree;
            this.changed = changed;
        }

        static Mutated unchanged(PairTree pairTree) {
            return new Mutated(pairTree, false);
        }

        static Mutated updated(PairTree pairTree) {
            return new Mutated(pairTree, true);
        }

        public boolean isChanged() {
            return changed;
        }

        public PairTree get() {
            return pairTree;
        }
    }

}
