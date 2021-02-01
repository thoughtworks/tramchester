package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.id.StringIdFor;
import com.tramchester.domain.Platform;

import java.util.Optional;
import java.util.Set;

@ImplementedBy(TransportData.class)
public interface PlatformRepository {
    Set<Platform> getPlatforms();
    boolean hasPlatformId(StringIdFor<Platform> id);
    Platform getPlatform(StringIdFor<Platform> id);
    Optional<Platform> getPlatformById(StringIdFor<Platform> id);

}
