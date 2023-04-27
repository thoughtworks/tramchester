package com.tramchester.graph.search.routes;

import com.tramchester.domain.id.HasId;
import com.tramchester.domain.places.InterchangeStation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public interface QueryPathsWithDepth {
    boolean hasAny();

    int getDepth();

    interface QueryPath extends QueryPathsWithDepth {

        Stream<QueryPath> stream();

        boolean isValid(Function<InterchangeStation, Boolean> validator);

        int size();
    }

    class AnyOf implements QueryPath {
        private final Set<QueryPath> paths;

        AnyOf(Set<? extends QueryPath> paths) {
            this.paths = new HashSet<>();
            this.paths.addAll(paths);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyOf that = (AnyOf) o;
            return paths.equals(that.paths);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paths);
        }

        @Override
        public Stream<QueryPath> stream() {
            return paths.stream();
        }

        @Override
        public String toString() {
            return "AnyOfContained{" + toString(paths) +
                    '}';
        }

        private String toString(Set<QueryPath> paths) {
            StringBuilder output = new StringBuilder();
            paths.forEach(interchangePath -> {
                output.append(System.lineSeparator());
                output.append(interchangePath.toString());
            });
            output.append(System.lineSeparator());
            return output.toString();
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return paths.stream().anyMatch(path -> path.isValid(validator));
        }

        @Override
        public boolean hasAny() {
            return paths.stream().anyMatch(QueryPath::hasAny);
        }

        @Override
        public int getDepth() {
            Optional<Integer> anyMatch = paths.stream().map(QueryPath::getDepth).max(Integer::compareTo);
            return anyMatch.orElse(Integer.MAX_VALUE);
        }

        @Override
        public int size() {
            return paths.size();
        }
    }

    class BothOf implements QueryPath {
        private final QueryPath pathsA;
        private final QueryPath pathsB;

        public BothOf(QueryPath pathsA, QueryPath pathsB) {
            this.pathsA = pathsA;
            this.pathsB = pathsB;
        }

        public QueryPath getFirst() {
            return pathsA;
        }

        public QueryPath getSecond() {
            return pathsB;
        }

        @Override
        public Stream<QueryPath> stream() {
            return Stream.concat(pathsA.stream(), pathsB.stream());
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return pathsA.isValid(validator) && pathsB.isValid(validator);
        }

        @Override
        public boolean hasAny() {
            return pathsA.hasAny() && pathsB.hasAny();
        }

        @Override
        public int getDepth() {
            int contained = Math.max(pathsA.getDepth(), pathsB.getDepth());
            return contained + 1;
        }

        @Override
        public int size() {
            return pathsA.size() + pathsB.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BothOf that = (BothOf) o;
            return pathsA.equals(that.pathsA) && pathsB.equals(that.pathsB);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pathsA, pathsB);
        }

        @Override
        public String toString() {
            return "BothOfPaths{" +
                    "pathsA=" + pathsA +
                    ", pathsB=" + pathsB +
                    '}';
        }

    }

    class ZeroPaths implements QueryPath {

        private static final ZeroPaths theInstance = new ZeroPaths();

        public static ZeroPaths get() {
            return theInstance;
        }

        private ZeroPaths() {

        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return false;
        }

        @Override
        public boolean hasAny() {
            return false;
        }

        @Override
        public int getDepth() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public Stream<QueryPath> stream() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "ZeroPaths{}";
        }
    }

    class AnyOfInterchanges implements QueryPath {
        // any of these changes being available makes the path valid
        private final Set<InterchangeStation> changes;

        public AnyOfInterchanges(Set<InterchangeStation> changes) {
            if (changes==null) {
                throw new RuntimeException("Cannot pass in null changes");
            }
            if (changes.isEmpty()) {
                throw new RuntimeException("Cannot pass in no interchanges");
            }
            if (changes.size()==1) {
                throw new RuntimeException("Not for 1 change " + HasId.asIds(changes));
            }
            this.changes = changes;
        }

        public static QueryPath Of(Set<InterchangeStation> changes) {
            if (changes.size()==1) {
                InterchangeStation change = changes.iterator().next();
                return new SingleInterchange(change);
            } else {
                return new AnyOfInterchanges(changes);
            }
        }

        @Override
        public String toString() {
            return "AnyOfInterchanges{" +
                    "changes=" + HasId.asIds(changes) +
                    '}';
        }

        @Override
        public Stream<QueryPath> stream() {
            return changes.stream().map(SingleInterchange::new);
        }

        @Override
        public boolean hasAny() {
            return true;
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public int size() {
            return changes.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AnyOfInterchanges that = (AnyOfInterchanges) o;
            return changes.equals(that.changes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(changes);
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return changes.stream().anyMatch(validator::apply);
        }
    }

    class SingleInterchange implements QueryPath {

        private final InterchangeStation interchangeStation;

        public SingleInterchange(InterchangeStation interchangeStation) {
            this.interchangeStation = interchangeStation;
        }

        @Override
        public Stream<QueryPath> stream() {
            return Stream.of(new SingleInterchange(interchangeStation));
        }

        @Override
        public boolean isValid(Function<InterchangeStation, Boolean> validator) {
            return validator.apply(interchangeStation);
        }

        @Override
        public boolean hasAny() {
            return true;
        }

        @Override
        public int getDepth() {
            return 1;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SingleInterchange that = (SingleInterchange) o;
            return interchangeStation.equals(that.interchangeStation);
        }

        @Override
        public int hashCode() {
            return Objects.hash(interchangeStation);
        }

        @Override
        public String toString() {
            return "SingleInterchange{" +
                    "change=" + interchangeStation.getStationId() +
                    '}';
        }
    }




}
