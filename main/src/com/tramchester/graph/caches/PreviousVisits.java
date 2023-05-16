package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.diagnostics.ReasonCode;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tramchester.graph.search.diagnostics.ReasonCode.*;

public class PreviousVisits implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PreviousVisits.class);

    private static final int CACHE_DURATION_MINS = 5;

    private final Cache<Long, ReasonCode> timeNodePrevious;
    private final Cache<Key<TramTime>, ReasonCode> hourNodePrevious;
    private final Cache<Long, ReasonCode> routeStationPrevious;
    private final Cache<Long, ReasonCode> servicePrevious;
    private final Cache<Long, Integer> lowestNumberOfChanges;

    public PreviousVisits() {
        timeNodePrevious = createCache(100000);
        hourNodePrevious = createCache(400000);
        routeStationPrevious = createCache(40000);
        servicePrevious = createCache(30000);
        lowestNumberOfChanges = createCache(20000);
    }

    @NotNull
    private <KEY, VALUE> Cache<KEY, VALUE> createCache(long maxCacheSize) {
        return Caffeine.newBuilder().maximumSize(maxCacheSize).expireAfterAccess(CACHE_DURATION_MINS, TimeUnit.MINUTES).
                recordStats().build();
    }

    public void recordVisitIfUseful(ReasonCode result, Node node, ImmutableJourneyState journeyState, EnumSet<GraphLabel> labels) {
        if (labels.contains(GraphLabel.MINUTE) || labels.contains(GraphLabel.HOUR)) {
            // time and hour nodes represent the time on the actual journey, so if we have been here before
            // we will get the same result
            TramTime journeyClock = journeyState.getJourneyClock();

            switch (result) {
                case DoesNotOperateOnTime -> timeNodePrevious.put(node.getId(), result);
                case NotAtHour -> hourNodePrevious.put(new Key<>(node, journeyClock), result);
            }
            return;
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            recordRouteStationVisitIfUseful(result, node.getId(), journeyState);
            return;
        }

        if (labels.contains(GraphLabel.SERVICE)) {
            if (result == NotOnQueryDate) {
                // the service is unavailable for the query date
                TramTime journeyClock = journeyState.getJourneyClock();
                boolean isNextDay = journeyClock.isNextDay();
                if (!isNextDay) {
                    servicePrevious.put(node.getId(), result);
                }
            }
        }
    }

    private void recordRouteStationVisitIfUseful(final ReasonCode result, final long nodeId, final ImmutableJourneyState journeyState) {
        if (result == TooManyRouteChangesRequired) {
            // based on a route->route changes count only, invariant on current state of a journey
            routeStationPrevious.put(nodeId, result);
        }
        if (result == RouteNotOnQueryDate) {
            // the route is unavailable for the query date
            TramTime journeyClock = journeyState.getJourneyClock();
            final boolean isNextDay = journeyClock.isNextDay();
            if (!isNextDay) {
                routeStationPrevious.put(nodeId, result);
            }
        }
        if (result == TooManyInterchangesRequired) {
            // too many changes, record lowest number of changes on journey that gave this result
            routeStationPrevious.put(nodeId, result);

            final int numberChanges = journeyState.getNumberChanges();
            final Integer currentLowest = lowestNumberOfChanges.getIfPresent(nodeId);
            if (currentLowest==null) {
                lowestNumberOfChanges.put(nodeId, numberChanges);
            } else {
                if (numberChanges < currentLowest) {
                    lowestNumberOfChanges.put(nodeId, numberChanges);
                }
            }
        }
    }

    public ReasonCode getPreviousResult(final Node node, final ImmutableJourneyState journeyState, final EnumSet<GraphLabel> labels) {

        if (labels.contains(GraphLabel.MINUTE)) {
            // time node has by definition a unique time
            final ReasonCode timeFound = timeNodePrevious.getIfPresent(node.getId());
            if (timeFound != null) {
                return timeFound;
            }
        }

        if (labels.contains(GraphLabel.HOUR)) {
            final ReasonCode hourFound = hourNodePrevious.getIfPresent(new Key<>(node, journeyState.getJourneyClock()));
            if (hourFound != null) {
                return hourFound;
            }
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            final ReasonCode found = routeStationPrevious.getIfPresent(node.getId());
            if (found != null) {
                if (found == TooManyInterchangesRequired) {
                    final Integer currentLowest = lowestNumberOfChanges.getIfPresent(node.getId());
                    if (currentLowest!=null && journeyState.getNumberChanges()>=currentLowest) {
                        return found;
                    }
                } else {
                    return found;
                }
            }
        }

        if (labels.contains(GraphLabel.SERVICE)) {
            final ReasonCode found = servicePrevious.getIfPresent(node.getId());
            if (found != null) {
                return found;
            }
        }

        return ReasonCode.PreviousCacheMiss;
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String, CacheStats>> results = new ArrayList<>();
        results.add(Pair.of("timeNodePrevious", timeNodePrevious.stats()));
        results.add(Pair.of("hourNodePrevious", hourNodePrevious.stats()));
        results.add(Pair.of("routeStationPrevious", routeStationPrevious.stats()));
        results.add(Pair.of("servicePrevious", servicePrevious.stats()));
        results.add(Pair.of("lowestNumberOfChanges", lowestNumberOfChanges.stats()));

        return results;
    }

    public void reportStats() {
        stats().forEach(pair -> logger.info("Cache stats for " + pair.getLeft() + " " + pair.getRight().toString()));
    }

    private static class Key<T> {

        private final long nodeId;
        private final T other;

        public Key(Node node, T other) {
            this.nodeId = node.getId();
            this.other = other;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key<?> key = (Key<?>) o;

            if (nodeId != key.nodeId) return false;
            return other.equals(key.other);
        }

        @Override
        public int hashCode() {
            int result = (int) (nodeId ^ (nodeId >>> 32));
            result = 31 * result + other.hashCode();
            return result;
        }
    }

}
