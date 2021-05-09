package com.tramchester.graph.search.stateMachine;

import com.tramchester.graph.search.stateMachine.states.TraversalState;

import java.util.HashMap;

public class RegistersStates implements RegistersFromState {

    HashMap<FromTo, TowardsState<? extends TraversalState>> map;

    public RegistersStates() {
        map = new HashMap<>();
    }

    public TowardsState<? extends TraversalState> getBuilderFor(Class<? extends TraversalState> from, Class<? extends TraversalState> to) {
        return map.get(new FromTo(from, to));
    }

    public void addBuilder(TowardsState<? extends TraversalState> builder) {
        builder.register(this);
    }

    @Override
    public <T extends TraversalState> void add(Class<? extends TraversalState> from, TowardsState<T> builder) {
        map.put(new FromTo(from, builder.getDestination()), builder);
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
    }
}
