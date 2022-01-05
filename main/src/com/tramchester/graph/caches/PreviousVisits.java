package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.ServiceReason;
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

import static com.tramchester.graph.search.ServiceReason.ReasonCode.*;

public class PreviousVisits implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PreviousVisits.class);

    private static final int CACHE_DURATION_MINS = 5;

    private final Cache<Long, ServiceReason.ReasonCode> timeNodePrevious;
    private final Cache<Key<TramTime>, ServiceReason.ReasonCode> hourNodePrevious;
    private final Cache<Long, ServiceReason.ReasonCode> routeStationPrevious;
    private final Cache<Long, ServiceReason.ReasonCode> servicePrevious;

    public PreviousVisits() {
        timeNodePrevious = createCache(100000);
        hourNodePrevious = createCache(400000);
        routeStationPrevious = createCache(40000);
        servicePrevious = createCache(30000);
    }

    @NotNull
    private <KEY> Cache<KEY, ServiceReason.ReasonCode> createCache(long maxCacheSize) {
        return Caffeine.newBuilder().maximumSize(maxCacheSize).expireAfterAccess(CACHE_DURATION_MINS, TimeUnit.MINUTES).
                recordStats().build();
    }

    public void recordVisitIfUseful(ServiceReason.ReasonCode result, Node node, ImmutableJourneyState journeyState, EnumSet<GraphLabel> labels) {
        if (labels.contains(GraphLabel.MINUTE) || labels.contains(GraphLabel.HOUR)) {
            TramTime journeyClock = journeyState.getJourneyClock();

            switch (result) {
                case DoesNotOperateOnTime -> timeNodePrevious.put(node.getId(), result);
                case NotAtHour -> hourNodePrevious.put(new Key<>(node, journeyClock), result);
            }
            return;
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
//            if (result == TooManyInterchangesAlready) {
//                routeStationPrevious.put(node.getId(), result);
//            }
            if (result == RouteNotOnQueryDate) {
                TramTime journeyClock = journeyState.getJourneyClock();
                boolean isNextDay = journeyClock.isNextDay();
                if (!isNextDay) {
                    routeStationPrevious.put(node.getId(), result);
                }
            }
            return;
        }

        if (labels.contains(GraphLabel.SERVICE)) {
            if (result == NotOnQueryDate) {
                TramTime journeyClock = journeyState.getJourneyClock();
                boolean isNextDay = journeyClock.isNextDay();
                if (!isNextDay) {
                    servicePrevious.put(node.getId(), result);
                }
            }
        }
    }

    public ServiceReason.ReasonCode getPreviousResult(Node node, ImmutableJourneyState journeyState, EnumSet<GraphLabel> labels) {

        if (labels.contains(GraphLabel.MINUTE)) {
            // time node has by definition a unique time
            ServiceReason.ReasonCode timeFound = timeNodePrevious.getIfPresent(node.getId());
            if (timeFound != null) {
                return timeFound;
            }
        }

        if (labels.contains(GraphLabel.HOUR)) {
            ServiceReason.ReasonCode hourFound = hourNodePrevious.getIfPresent(new Key<>(node, journeyState.getJourneyClock()));
            if (hourFound != null) {
                return hourFound;
            }
        }

        if (labels.contains(GraphLabel.ROUTE_STATION)) {
            ServiceReason.ReasonCode found = routeStationPrevious.getIfPresent(node.getId());
            if (found != null) {
                return found;
            }
        }

        if (labels.contains(GraphLabel.SERVICE)) {
            ServiceReason.ReasonCode found = servicePrevious.getIfPresent(node.getId());
            if (found != null) {
                return found;
            }
        }

        return ServiceReason.ReasonCode.PreviousCacheMiss;
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String, CacheStats>> results = new ArrayList<>();
        results.add(Pair.of("timeNodePrevious", timeNodePrevious.stats()));
        results.add(Pair.of("hourNodePrevious", hourNodePrevious.stats()));
        results.add(Pair.of("routeStationPrevious", routeStationPrevious.stats()));
        results.add(Pair.of("servicePrevious", servicePrevious.stats()));

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
