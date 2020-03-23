package com.tramchester.repository;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface ReportsCacheStats {
    List<Pair<String, CacheStats>> stats();
}
