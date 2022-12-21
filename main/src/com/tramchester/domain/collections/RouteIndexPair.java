package com.tramchester.domain.collections;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RouteIndexPair {
    private final int first;
    private final int second;
    private final int hashCode;

    private RouteIndexPair(int first, int second) {
        this.first = first;
        this.second = second;
        hashCode = first *31 + second; //Objects.hash(first, second);
    }

    public static RouteIndexPair of(int first, int second) {
        return new RouteIndexPair(first, second);
    }

    public int first() {
        return first;
    }

    public int second() {
        return second;
    }

    public boolean isSame() {
        return first == second;
    }

    @Override
    public String toString() {
        return "RouteIndexPair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteIndexPair routePair = (RouteIndexPair) o;
        return first == routePair.first && second == routePair.second;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }


    public static List<Group> createAllUniqueGroups(Stream<RouteIndexPair> pairsToGroup) {
        Map<Integer, Set<RouteIndexPair>> byFirst = new HashMap<>();
        Map<Integer, Set<RouteIndexPair>> bySecond = new HashMap<>();

        pairsToGroup.forEach(routeIndexPair -> {
            int first = routeIndexPair.first;
            int second = routeIndexPair.second;
            if (!byFirst.containsKey(first)) {
                byFirst.put(first, new HashSet<>());
            }
            byFirst.get(first).add(routeIndexPair);
            if (!bySecond.containsKey(second)) {
                bySecond.put(second, new HashSet<>());
            }
            bySecond.get(second).add(routeIndexPair);
        });

        List<Group> results = new ArrayList<>();

        byFirst.forEach((first, items) -> {
            // match on first of one, second of other i.e. 1,3 3,5
            if (bySecond.containsKey(first)) {
                bySecond.get(first).forEach(pairA -> {
                    items.forEach(pairB -> {
                        Group matched = Group.of(pairA, pairB);
                        results.add(matched);
                    });
                });
            }
        });

        byFirst.clear();
        bySecond.clear();

        return results;
    }

    public interface PairTree {
        List<RouteIndexPair> flatten();

        PairTree replace(RouteIndexPair toReplace, RouteIndexPair pairA, RouteIndexPair pairB);

        Set<PairTree> visit(TreeVisitor visitor);
    }

    // TODO Mutated Flag passed in??

    public static class PairTreeLeaf implements PairTree {
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
        public Set<PairTree> visit(TreeVisitor visitor) {
            return visitor.visit(this);
        }

        public RouteIndexPair get() {
            return leaf;
        }

        @Override
        public String toString() {
            return "("+leaf.first()+","+leaf.second()+")";
        }
    }

    public static class PairTreeBranch implements PairTree {
        private final PairTree left;
        private final PairTree right;
        private final int hash;

        public PairTreeBranch(PairTree left, PairTree right) {
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
        public Set<PairTree> visit(TreeVisitor visitor) {
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

    public interface TreeVisitor {
        Set<PairTree> visit(PairTreeLeaf tree);
    }

    public static class Group {
        final RouteIndexPair first;
        final RouteIndexPair second;

        public Group(RouteIndexPair first, RouteIndexPair second) {
            this.first = first;
            this.second = second;
        }

        public static Group of(RouteIndexPair pairA, RouteIndexPair pairB) {
            return new Group(pairA, pairB);
        }

        public RouteIndexPair first() {
            return first;
        }

        public RouteIndexPair second() {
            return second;
        }

        @Override
        public String toString() {
            return "Group{" +
                     first +
                    ", " + second +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Group group = (Group) o;
            return first.equals(group.first) && second.equals(group.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }
    }
}
