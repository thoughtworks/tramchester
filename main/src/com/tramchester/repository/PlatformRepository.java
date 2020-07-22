package com.tramchester.repository;

import com.tramchester.domain.Platform;

import java.util.Optional;
import java.util.Set;

public interface PlatformRepository {
    Set<Platform> getPlatforms();
    boolean hasPlatformId(String id);
    Platform getPlatform(String id);

    Optional<Platform> getPlatformById(String platformId);
}
