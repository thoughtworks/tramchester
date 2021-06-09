package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.JourneyRequest;
import com.tramchester.graph.search.ServiceReason;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreviousSuccessfulVisits implements ReportsCacheStats {
    private static final Logger logger = LoggerFactory.getLogger(PreviousSuccessfulVisits.class);
    private static final long MAX_CACHE_SZIE = 10000;
    private static final int CACHE_DURATION_MINS = 30;

    private final Cache<Long, ServiceReason.ReasonCode> timeNodePrevious;
    private final Cache<NodeIdAndTime, ServiceReason.ReasonCode> hourNodePrevious;
    private int lowestCost;

    public PreviousSuccessfulVisits() {
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

    public void recordVisitIfUseful(ServiceReason.ReasonCode result, Long nodeId, TramTime journeyClock) {
        switch (result) {
            case NotAtQueryTime, TimeOk -> timeNodePrevious.put(nodeId, result);
            case HourOk, NotAtHour -> hourNodePrevious.put(new NodeIdAndTime(nodeId, journeyClock), result);
        }
    }

    public ServiceReason.ReasonCode getPreviousResult(Long nodeId, TramTime journeyClock) {

        ServiceReason.ReasonCode timeFound = timeNodePrevious.getIfPresent(nodeId);
        if (timeFound != null) {
            return timeFound;
        }

        ServiceReason.ReasonCode hourFound = hourNodePrevious.getIfPresent(new NodeIdAndTime(nodeId, journeyClock));
        if (hourFound != null) {
            return hourFound;
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
