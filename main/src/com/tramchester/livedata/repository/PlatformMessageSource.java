package com.tramchester.livedata.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Platform;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ImplementedBy(PlatformMessageRepository.class)
public interface PlatformMessageSource extends LiveDataCache {
    Optional<PlatformMessage> messagesFor(IdFor<Platform> platformId, LocalDate queryDate, TramTime queryTime);
    List<PlatformMessage> messagesFor(Station station, LocalDate queryDate, TramTime queryTime);

    boolean isEnabled();
}
