package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.Platform;

import java.util.Optional;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface PlatformRepository {
    Set<Platform> getPlatforms();
    boolean hasPlatformId(IdFor<Platform> id);
    Platform getPlatform(IdFor<Platform> id);
    Optional<Platform> getPlatformById(IdFor<Platform> id);

}
