package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;

import java.util.Set;

@ImplementedBy(TransportData.class)
public interface PlatformRepository {
    Set<Platform> getPlatforms();
    boolean hasPlatformId(IdFor<Platform> id);
    Platform getPlatformById(IdFor<Platform> id);

}
