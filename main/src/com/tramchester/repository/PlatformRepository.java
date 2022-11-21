package com.tramchester.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.reference.TransportMode;

import java.util.Set;
import java.util.stream.Stream;

@ImplementedBy(TransportData.class)
public interface PlatformRepository {
    Set<Platform> getPlatforms(Set<TransportMode> modes);
    boolean hasPlatformId(IdFor<Platform> id);
    Platform getPlatformById(IdFor<Platform> id);

    Stream<Platform> getPlaformStream();
}
