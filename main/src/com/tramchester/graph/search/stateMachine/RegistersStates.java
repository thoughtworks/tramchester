package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.TraversalState;

import java.util.HashMap;

public class RegistersStates implements RegistersFromState {

    HashMap<FromTo, TowardsState<? extends TraversalState>> map;

    public RegistersStates() {
        map = new HashMap<>();
    }

    public <T extends TraversalState> TowardsState<T> getBuilderFor(Class<? extends TraversalState> from, Class<T> to) {
        final FromTo key = new FromTo(from, to);

        if (!map.containsKey(key)) {
            throw new NextStateNotFoundException(key);
        }
        return (TowardsState<T>) map.get(key);

    }

    public void addBuilder(TowardsState<? extends TraversalState> builder) {
        builder.register(this);
    }

    @Override
    public <T extends TraversalState> void add(Class<? extends TraversalState> from, TowardsState<T> builder) {
        map.put(new FromTo(from, builder.getDestination()), builder);
    }

    public void clear() {
        map.clear();
    }

    public static class FromTo {

        private final Class<? extends TraversalState> from;
        private final Class<? extends TraversalState> to;

        public FromTo(Class<? extends TraversalState> from, Class<? extends TraversalState> to) {

            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FromTo fromTo = (FromTo) o;

            if (!from.equals(fromTo.from)) return false;
            return to.equals(fromTo.to);
        }

        @Override
        public int hashCode() {
            int result = from.hashCode();
            result = 31 * result + to.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "FromTo{" +
                    "from=" + from +
                    ", to=" + to +
                    '}';
        }
    }
}
