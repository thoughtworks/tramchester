package com.tramchester.livedata.repository;

import com.google.inject.ImplementedBy;
import com.tramchester.domain.Platform;
import com.tramchester.domain.dates.TramDate;
import com.tramchester.domain.id.IdFor;
import com.tramchester.domain.places.Station;
import com.tramchester.domain.time.TramTime;
import com.tramchester.livedata.domain.liveUpdates.PlatformMessage;
import com.tramchester.livedata.tfgm.PlatformMessageRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ImplementedBy(PlatformMessageRepository.class)
public interface PlatformMessageSource extends TramLiveDataCache {
    Optional<PlatformMessage> messagesFor(IdFor<Platform> platformId, TramDate queryDate, TramTime queryTime);
    List<PlatformMessage> messagesFor(Station station, TramDate queryDate, TramTime queryTime);

    boolean isEnabled();
}
