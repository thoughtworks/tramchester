package com.tramchester;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class CacheMetricSet {

    private final List<ReportsCacheStats> haveCacheStats;
    private final MetricRegistry registry;

    public CacheMetricSet(List<ReportsCacheStats> haveCacheStats, MetricRegistry registry) {
        this.haveCacheStats = haveCacheStats;
        this.registry = registry;
    }

    public void prepare() {
        haveCacheStats.forEach(reportsCacheStats -> register(registry, reportsCacheStats));
    }

    private void register(MetricRegistry registry, ReportsCacheStats reportsCacheStats) {
        List<Pair<String, CacheStats>> cacheStats = reportsCacheStats.stats();
        cacheStats.forEach(cacheStat -> register(reportsCacheStats, cacheStat, registry));
    }

    private void register(ReportsCacheStats owningClass, Pair<String, CacheStats> cacheStat, MetricRegistry registry) {
        registerNamedMetric(registry, owningClass, cacheStat, "hitRate", CacheStats::hitRate);
        registerNamedMetric(registry, owningClass, cacheStat, "missRate", CacheStats::missRate);
        registerNamedMetric(registry, owningClass, cacheStat, "hitCount", CacheStats::hitCount);
        registerNamedMetric(registry, owningClass, cacheStat, "missCount", CacheStats::missCount);
    }

    private <T> void registerNamedMetric(MetricRegistry registry, ReportsCacheStats owningClass, Pair<String, CacheStats> cacheStat,
                                     String counterName, Function<CacheStats, T> function) {
        String cacheName = cacheStat.getLeft();
        String metricName = MetricRegistry.name(owningClass.getClass(), cacheName, counterName);
        registry.register(metricName, (Gauge<T>) () -> getStatFor(owningClass, cacheName, function));
    }

    private <T> T getStatFor(ReportsCacheStats owningClass, String cacheName, Function<CacheStats, T> getStat) {
        List<Pair<String, CacheStats>> classStats = owningClass.stats();
        Optional<T> value = classStats.stream().
                filter(pair -> pair.getLeft().equals(cacheName)).
                map(Pair::getRight).map(getStat).limit(1).findFirst();
        return value.get();
    }


}
