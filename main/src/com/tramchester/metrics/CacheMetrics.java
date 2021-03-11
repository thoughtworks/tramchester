package com.tramchester.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.tramchester.repository.ReportsCacheStats;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;

import static java.lang.String.format;

@LazySingleton
public class CacheMetrics {
    private static final Logger logger = LoggerFactory.getLogger(CacheMetrics.class);

    private final Set<ReportsCacheStats> registered;
    private final RegistersCacheMetrics registry;

    public static class DropWizardMetrics implements RegistersCacheMetrics {
        private final MetricRegistry registry;

        public DropWizardMetrics(MetricRegistry registry) {
            this.registry = registry;
        }

        @Override
        public <T> void register(String metricName, Gauge<T> gauge) {
            registry.register(metricName, gauge);
        }
    }

    public interface RegistersCacheMetrics {
        <T> void register(String metricName, Gauge<T> Gauge);
    }

    @Inject
    public CacheMetrics(RegistersCacheMetrics registry) {
        this.registry = registry;
        registered = new HashSet<>();
    }

    public void register(ReportsCacheStats reportsCacheStats) {
        logger.info("Registered "  + reportsCacheStats.toString());
        register(registry, reportsCacheStats);
        registered.add(reportsCacheStats);
    }

    private void register(RegistersCacheMetrics registry, ReportsCacheStats reportsCacheStats) {
        List<Pair<String, CacheStats>> cacheStats = reportsCacheStats.stats();
        cacheStats.forEach(cacheStat -> register(reportsCacheStats, cacheStat, registry));
    }

    private void register(ReportsCacheStats owningClass, Pair<String, CacheStats> cacheStat, RegistersCacheMetrics registry) {
        registerNamedMetric(registry, owningClass, cacheStat, "hitRate", CacheStats::hitRate);
        registerNamedMetric(registry, owningClass, cacheStat, "missRate", CacheStats::missRate);
        registerNamedMetric(registry, owningClass, cacheStat, "hitCount", CacheStats::hitCount);
        registerNamedMetric(registry, owningClass, cacheStat, "missCount", CacheStats::missCount);
    }

    private <T> void registerNamedMetric(RegistersCacheMetrics registry, ReportsCacheStats owningClass, Pair<String, CacheStats> cacheStat,
                                         String counterName, Function<CacheStats, T> function) {
        String cacheName = cacheStat.getLeft();
        String metricName = MetricRegistry.name(owningClass.getClass(), cacheName, counterName);
        registry.register(metricName, () -> getStatFor(owningClass, cacheName, function));
    }

    private <T> T getStatFor(ReportsCacheStats owningClass, String cacheName, Function<CacheStats, T> getStat) {
        List<Pair<String, CacheStats>> classStats = owningClass.stats();
        Optional<T> value = classStats.stream().
                filter(pair -> pair.getLeft().equals(cacheName)).
                map(Pair::getRight).map(getStat).limit(1).findFirst();
        return value.get();
    }

    public void report() {
        registered.forEach(component -> reportCacheStats(component.getClass().getSimpleName(), component.stats()));
    }

    private void reportCacheStats(String className, List<Pair<String, CacheStats>> stats) {
        stats.forEach(stat -> logger.info(format("%s: %s: %s", className, stat.getLeft(), stat.getRight().toString())));
    }

}
