package com.tramchester.graph.caches;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tramchester.domain.time.TramTime;
import com.tramchester.graph.search.ServiceReason;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class PreviousSuccessfulVisits {
    private static final long MAX_CACHE_SZIE = 10000;
    private static final int CACHE_DURATION_MINS = 30;

    private final Cache<Long, ServiceReason.ReasonCode> timeNodePrevious;
    private final Cache<NodeIdAndTime, ServiceReason.ReasonCode> hourNodePrevious;

    public PreviousSuccessfulVisits() {
        timeNodePrevious = createCache();
        hourNodePrevious = createCache();
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

    public boolean isMultipleJourneyMode() {
        return false;
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

    private static class NodeIdAndTime {

        private final long nodeId;
        private final TramTime journeyClock;

        public NodeIdAndTime(long nodeId, TramTime journeyClock) {
            this.nodeId = nodeId;
            this.journeyClock = journeyClock;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeIdAndTime that = (NodeIdAndTime) o;

            if (nodeId != that.nodeId) return false;
            return journeyClock.equals(that.journeyClock);
        }

        @Override
        public int hashCode() {
            int result = (int) (nodeId ^ (nodeId >>> 32));
            result = 31 * result + journeyClock.hashCode();
            return result;
        }
    }
}
