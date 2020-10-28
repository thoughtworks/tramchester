package com.tramchester.domain.liveUpdates;

import com.tramchester.domain.IdFor;
import com.tramchester.domain.Platform;
import com.tramchester.domain.places.Station;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class PlatformDueTrams {
    private final IdFor<Platform> stationPlatform;
    private final List<DueTram> dueTrams;
    private final LocalDateTime lastUpdate;
    private final IdFor<Station> stationId;

    public PlatformDueTrams(IdFor<Platform> stationPlatform, List<DueTram> dueTrams, LocalDateTime lastUpdate, IdFor<Station> stationId) {
        this.stationPlatform = stationPlatform;
        this.dueTrams = dueTrams;
        this.lastUpdate = lastUpdate;
        this.stationId = stationId;
    }

    public PlatformDueTrams(StationDepartureInfo departureInfo) {
        this(departureInfo.getStationPlatform(), departureInfo.getDueTrams(), departureInfo.getLastUpdate(),
                departureInfo.getStation().getId());
    }

    public boolean hasDueTram(DueTram dueTram) {
        return dueTrams.contains(dueTram);
    }

    public void addDueTram(DueTram dueTram) {
        dueTrams.add(dueTram);
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public List<DueTram> getDueTrams() {
        return dueTrams;
    }

    public List<DueTram> getDueTramsWithinWindow(int minutes) {
        return dueTrams.stream().filter(dueTram -> dueTram.getWait()<=minutes).collect(Collectors.toList());
    }
}

