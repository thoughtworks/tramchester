package com.tramchester.repository;

import com.tramchester.domain.Platform;

import java.util.Optional;

public interface PlatformRepository {
    Optional<Platform> getPlatformById(String platformId);
}
