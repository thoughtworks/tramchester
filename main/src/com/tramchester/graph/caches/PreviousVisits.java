package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.ServiceReason;
import com.tramchester.graph.search.TramRouteEvaluator;
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

public class PreviousVisits implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PreviousVisits.class);

    // Only per stream at the moment, so likely deleted before ever hitting this time
    private static final int CACHE_DURATION_MINS = 10;

    private final NodeContentsRepository contentsRepository;
    private final Cache<Long, ServiceReason.ReasonCode> timeNodePrevious;
    private final Cache<Key<TramTime>, ServiceReason.ReasonCode> hourNodePrevious;
    private final Cache<Key<ImmutableJourneyState>, ServiceReason.ReasonCode> previousNodes;
    private int lowestCost;

    public PreviousVisits(NodeContentsRepository contentsRepository) {
        this.contentsRepository = contentsRepository;
        timeNodePrevious = createCache(100000);
        hourNodePrevious = createCache(400000);
        previousNodes = createCache(400000);
        lowestCost = Integer.MAX_VALUE;
    }

    @NotNull
    private <KEY> Cache<KEY, ServiceReason.ReasonCode> createCache(long maxCacheSize) {
        return Caffeine.newBuilder().maximumSize(maxCacheSize).expireAfterAccess(CACHE_DURATION_MINS, TimeUnit.MINUTES).
                recordStats().build();
    }

    public void clear() {
        timeNodePrevious.invalidateAll();
        hourNodePrevious.invalidateAll();
        previousNodes.invalidateAll();
    }

    public void recordVisitIfUseful(ServiceReason.ReasonCode result, Node node, ImmutableJourneyState journeyState) {
        EnumSet<GraphLabel> labels = contentsRepository.getLabels(node);
        if (labels.contains(GraphLabel.MINUTE) || labels.contains(GraphLabel.HOUR)) {
            TramTime journeyClock = journeyState.getJourneyClock();
            //
            switch (result) {
                case NotAtQueryTime -> timeNodePrevious.put(node.getId(), result);
                case NotAtHour -> hourNodePrevious.put(new Key<>(node, journeyClock), result);
            }
        } else if (labels.contains(GraphLabel.ROUTE_STATION)) {
            if (!TramRouteEvaluator.decideEvaluationAction(result).continues()) {
                previousNodes.put(new Key<>(node, journeyState), result);
            }
        }
    }

    public ServiceReason.ReasonCode getPreviousResult(Node node, ImmutableJourneyState journeyState) {

        EnumSet<GraphLabel> labels = contentsRepository.getLabels(node);

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

        ServiceReason.ReasonCode nodeFound = previousNodes.getIfPresent(new Key<>(node, journeyState));
        if (nodeFound != null) {
            return nodeFound;
        }


        return ServiceReason.ReasonCode.PreviousCacheMiss;
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String, CacheStats>> results = new ArrayList<>();
        results.add(Pair.of("timeNodePrevious", timeNodePrevious.stats()));
        results.add(Pair.of("hourNodePrevious", hourNodePrevious.stats()));
        results.add(Pair.of("previousNodes", previousNodes.stats()));
        return results;
    }

    public void reportStatsFor(JourneyRequest journeyRequest) {
        logger.info("Cache stats for " + journeyRequest.getUid());
        stats().forEach(pair -> logger.info("Cache stats for " + pair.getLeft() + " " + pair.getRight().toString()));
    }

    public int getLowestCost() {
        return lowestCost;
    }

    public void setLowestCost(int cost) {
        this.lowestCost = cost;
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
