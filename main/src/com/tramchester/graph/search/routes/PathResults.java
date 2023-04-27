package com.tramchester.graph.search.routes;

import com.tramchester.domain.places.InterchangeStation;

import java.util.function.Function;
import java.util.stream.Stream;

public interface PathResults { 

    boolean hasAny();

    int getDepth();

    int numberPossible();

    boolean isValid(Function<InterchangeStation, Boolean> valid);

    Stream<QueryPathsWithDepth.QueryPath> stream();

    class HasPathResults implements PathResults {
        private final QueryPathsWithDepth.QueryPath pathFor;

        public HasPathResults(QueryPathsWithDepth.QueryPath pathFor) {
            this.pathFor = pathFor;
        }

        public boolean hasAny() {
            return pathFor.stream().anyMatch(QueryPathsWithDepth.QueryPath::hasAny);
        }

        public int getDepth() {
            return pathFor.getDepth();
        }

        public int numberPossible() {
            return pathFor.size();
        }

        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return pathFor.stream().anyMatch(path -> pathFor.isValid(validator));
        }

        public Stream<QueryPathsWithDepth.QueryPath> stream() {
            return pathFor.stream().filter(QueryPathsWithDepth::hasAny);
        }
    }

    class NoPathResults implements PathResults {

        @Override
        public boolean hasAny() {
            return false;
        }

        @Override
        public int getDepth() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int numberPossible() {
            return 0;
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> valid) {
            return false;
        }

        @Override
        public Stream<QueryPathsWithDepth.QueryPath> stream() {
            return Stream.empty();
        }
    }

}
