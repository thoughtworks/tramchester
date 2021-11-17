package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.MutablePlatform;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;

import java.util.Optional;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface PlatformRepository {
    Set<MutablePlatform> getPlatforms();
    Platform getPlatform(IdFor<Platform> id);
    Optional<Platform> getPlatformById(IdFor<Platform> id);

}
