package com.tramchester.repository;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;

import java.util.Optional;
import java.util.Set;

public interface PlatformRepository {
    Set<Platform> getPlatforms();
    boolean hasPlatformId(IdFor<Platform> id);
    Platform getPlatform(IdFor<Platform> id);
    Optional<Platform> getPlatformById(IdFor<Platform> id);

    @Deprecated
    Optional<Platform> getPlatformById(String platformId);
}
