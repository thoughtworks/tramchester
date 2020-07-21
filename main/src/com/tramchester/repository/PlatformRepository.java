package com.tramchester.repository;

import com.tramchester.domain.Platform;

import java.util.Optional;
import java.util.Set;

public interface PlatformRepository {
    Optional<Platform> getPlatformById(String platformId);
    boolean hasPlatformId(String id);
    Platform getPlatform(String id);
    Set<Platform> getPlatforms();
}
