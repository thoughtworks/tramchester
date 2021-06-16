package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.graphbuild.GraphLabel;
import com.tramchester.graph.search.ImmutableJourneyState;
import com.tramchester.graph.search.JourneyRequest;
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

public class PreviousSuccessfulVisits implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PreviousSuccessfulVisits.class);

    private static final long MAX_CACHE_SZIE = 100000;

    // Only per stream at the moment, so likely deleted before ever hitting this time
    private static final int CACHE_DURATION_MINS = 10;

    private final NodeContentsRepository contentsRepository;
    private final Cache<Long, ServiceReason.ReasonCode> timeNodePrevious;
    private final Cache<NodeIdAndTime, ServiceReason.ReasonCode> hourNodePrevious;
    private int lowestCost;

    public PreviousSuccessfulVisits(NodeContentsRepository contentsRepository) {
        this.contentsRepository = contentsRepository;
        timeNodePrevious = createCache();
        hourNodePrevious = createCache();
        lowestCost = Integer.MAX_VALUE;
    }

    @NotNull
    private <KEY> Cache<KEY, ServiceReason.ReasonCode> createCache() {
        return Caffeine.newBuilder().maximumSize(MAX_CACHE_SZIE).expireAfterAccess(CACHE_DURATION_MINS, TimeUnit.MINUTES).
                recordStats().build();
    }

    public void clear() {
        timeNodePrevious.invalidateAll();
        hourNodePrevious.invalidateAll();
    }

    public void recordVisitIfUseful(ServiceReason.ReasonCode result, Node node, ImmutableJourneyState journeyState) {
        TramTime journeyClock = journeyState.getJourneyClock();
        switch (result) {
            case NotAtQueryTime, TimeOk -> timeNodePrevious.put(node.getId(), result);
            case HourOk, NotAtHour -> hourNodePrevious.put(new NodeIdAndTime(node.getId(), journeyClock), result);
        }
    }

    public ServiceReason.ReasonCode getPreviousResult(Node node, TramTime journeyClock) {

        EnumSet<GraphLabel> labels = contentsRepository.getLabels(node);

        if (labels.contains(GraphLabel.MINUTE)) {
            // time node has by definition a unique time
            ServiceReason.ReasonCode timeFound = timeNodePrevious.getIfPresent(node.getId());
            if (timeFound != null) {
                return timeFound;
            }
        }

        if (labels.contains(GraphLabel.HOUR)) {
            ServiceReason.ReasonCode hourFound = hourNodePrevious.getIfPresent(new NodeIdAndTime(node.getId(), journeyClock));
            if (hourFound != null) {
                return hourFound;
            }
        }

        return ServiceReason.ReasonCode.PreviousCacheMiss;
    }

    @Override
    public List<Pair<String, CacheStats>> stats() {
        List<Pair<String, CacheStats>> results = new ArrayList<>();
        results.add(Pair.of("timeNodePrevious", timeNodePrevious.stats()));
        results.add(Pair.of("hourNodePrevious", hourNodePrevious.stats()));
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

    private static class NodeIdAndTime {

        private final long nodeId;
        private final TramTime tramTime;

        public NodeIdAndTime(long nodeId, TramTime tramTime) {
            this.nodeId = nodeId;
            this.tramTime = tramTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeIdAndTime that = (NodeIdAndTime) o;

            if (nodeId != that.nodeId) return false;
            return tramTime.equals(that.tramTime);
        }

        @Override
        public int hashCode() {
            int result = (int) (nodeId ^ (nodeId >>> 32));
            result = 31 * result + tramTime.hashCode();
            return result;
        }
    }
}
