package com.tramchester.graph.search.stateMachine;

import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.graph.search.stateMachine.states.TraversalState;

import java.util.HashMap;
import java.util.Set;

@LazySingleton
public class RegistersStates implements RegistersFromState {

    HashMap<FromTo, Towards<? extends TraversalState>> map;

    public RegistersStates() {
        map = new HashMap<>();
    }

    public <S extends TraversalState, T extends Towards<S>> T getBuilderFor(Class<? extends TraversalState> from, Class<S> to) {
        final FromTo key = new FromTo(from, to);

        if (!map.containsKey(key)) {
            throw new NextStateNotFoundException(key);
        }
        return (T) map.get(key);

    }

    public void addBuilder(Towards<? extends TraversalState> builder) {
        builder.register(this);
    }

    @Override
    public <T extends TraversalState> void add(Class<? extends TraversalState> from, Towards<T> builder) {
        map.put(new FromTo(from, builder.getDestination()), builder);
    }

    public void clear() {
        map.clear();
    }

    public Set<FromTo> getTransitions() {
        return map.keySet();
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

        public Class<? extends TraversalState> getFrom() {
            return from;
        }

        public Class<? extends TraversalState> getTo() {
            return to;
        }
    }
}
